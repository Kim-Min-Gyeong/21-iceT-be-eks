package icet.koco.alarm.service;

import icet.koco.alarm.dto.AlarmListDto;
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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${BASE_URL}")
    private String baseUrl;

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

    public SseEmitter subscribe(Long userId, String lastEventId) {
        String emitterId = makeEmitterId(userId);
        SseEmitter emitter = new SseEmitter(60L * 1000 * 60); // 1시간 유효

        // emitter 등록
        emitterRepository.save(emitterId, emitter);

        // emitter 종료 처리 등록
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
        emitter.onError((e) -> emitterRepository.deleteById(emitterId));

        // 연결 성공 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("connect")
                    .data("connected"));
        } catch (IOException e) {
            throw new RuntimeException("SSE 연결 실패", e);
        }

        // 서버 재시작으로 인한 이벤트 누락 복원 (Last-Event-ID 기준 캐시 재전송)
        if (lastEventId != null) {
            Map<String, Object> cachedEvents = emitterRepository.findAllEventCacheByUserId(String.valueOf(userId));
            cachedEvents.entrySet().stream()
                    .filter(entry -> entry.getKey().compareTo(lastEventId) > 0)
                    .forEach(entry -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .id(entry.getKey())
                                    .name("alarm")
                                    .data(entry.getValue()));
                        } catch (IOException e) {
                            emitterRepository.deleteById(emitterId);
                        }
                    });
        }

        // 읽지 않은 알림(DB에 isRead = false)을 재전송
        List<Alarm> unreadAlarms = alarmRepository.findByReceiverIdAndIsReadFalse(userId);
        unreadAlarms.forEach(alarm -> {
            AlarmListDto.AlarmDto dto = AlarmListDto.AlarmDto.builder()
                    .id(alarm.getId())
                    .postId(alarm.getPost().getId())
                    .postTitle(alarm.getPost().getTitle())
                    .receiverId(alarm.getReceiver().getId())
                    .senderId(alarm.getSender().getId())
                    .senderNickname(alarm.getSender().getNickname())
                    .alarmType(alarm.getAlarmType())
                    .url(baseUrl + "/posts/" + alarm.getPost().getId())
                    .createdAt(alarm.getCreatedAt())
                    .build();

            try {
                emitter.send(SseEmitter.event()
                        .id(emitterId)
                        .name("alarm")
                        .data(dto));
                emitterRepository.saveEventCache(emitterId, dto); // 캐시 저장
            } catch (IOException e) {
                log.warn("❌ unread 알림 전송 실패: alarmId={}, error={}", alarm.getId(), e.getMessage());
            }
        });

        return emitter;
    }

    String makeEmitterId(Long userId) {
        return userId + "_" + System.currentTimeMillis();
    }
}
