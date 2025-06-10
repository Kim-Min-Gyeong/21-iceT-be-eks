package icet.koco.alarm.service;

import icet.koco.alarm.dto.AlarmListDto.AlarmDto;
import icet.koco.alarm.dto.AlarmRequestDto;
import icet.koco.alarm.emitter.EmitterRepository;
import icet.koco.alarm.entity.Alarm;
import icet.koco.alarm.repository.AlarmRepository;
import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.posts.entity.Post;
import icet.koco.posts.repository.PostRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {
    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final EmitterRepository emitterRepository;

    public void createAlarmInternal(AlarmRequestDto requestDto) {
        // 게시글, 알림 송수신자 찾기
        Post post = postRepository.findById(requestDto.getPostId())
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));

        User sender = userRepository.findByIdAndDeletedAtIsNull(requestDto.getSenderId())
            .orElseThrow(() -> new ResourceNotFoundException("사용자가 존재하지 않습니다. (AlarmSender) "));

        User receiver = userRepository.findByIdAndDeletedAtIsNull(post.getUser().getId())
            .orElseThrow(() -> new ResourceNotFoundException("사용자가 존재하지 않습니다. (AlarmReceiver) "));



        // 알림 생성
        Alarm alarm = Alarm.builder()
            .post(post)
            .sender(sender)
            .receiver(receiver)
            .alarmType(requestDto.getAlarmType())
            .createdAt(LocalDateTime.now())
            .build();

        alarmRepository.save(alarm);
        log.info("📌 알림 생성 완료: alarmId={}, receiverId={}, senderId={}",
            alarm.getId(), receiver.getId(), sender.getId());

        // SSE 알림 DTO 구성
        AlarmDto alarmDto = AlarmDto.builder()
            .id(alarm.getId())
            .postId(post.getId())
            .postTitle(post.getTitle())
            .receiverId(receiver.getId())
            .senderId(sender.getId())
            .senderNickname(sender.getNickname())
            .alarmType(requestDto.getAlarmType())
            .url(requestDto.getUrl())
            .createdAt(alarm.getCreatedAt())
            .build();

        // SSE로 전송
        String receiverKey = String.valueOf(receiver.getId());
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(receiverKey);
        log.info("📡 전송 대상 SSE emitter 수: {}", emitters.size());

        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("alarm")
                    .data(alarmDto));
                emitterRepository.saveEventCache(emitterId, alarmDto); // 이벤트 캐시 저장
                log.info("✅ 알림 전송 성공: emitterId={}", emitterId);
            } catch (IOException e) {
                log.warn("❌ 알림 전송 실패: emitterId={}, error={}", emitterId, e.getMessage());
                emitter.completeWithError(e);
                emitterRepository.deleteById(emitterId);
            }
        });
    }
}
