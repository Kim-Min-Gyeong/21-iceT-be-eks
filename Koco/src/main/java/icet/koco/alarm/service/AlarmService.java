package icet.koco.alarm.service;

import icet.koco.alarm.dto.AlarmRequestDto;
import icet.koco.alarm.entity.Alarm;
import icet.koco.alarm.repository.AlarmRepository;
import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.posts.entity.Post;
import icet.koco.posts.repository.PostRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlarmService {
    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public void createAlarm(Long userId, AlarmRequestDto requestDto) {
        // 게시글, 알람 송수신자 찾기
        User sender = userRepository.findByIdAndDeletedAtIsNull(requestDto.getSenderId())
            .orElseThrow(() -> new ResourceNotFoundException("사용자가 존재하지 않습니다. (AlarmSender) "));

        User receiver = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자가 존재하지 않습니다. (AlarmReceiver) "));

        Post post = postRepository.findById(requestDto.getPostId())
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));

        // 알람 생성
        Alarm alarm = Alarm.builder()
            .post(post)
            .sender(sender)
            .receiver(receiver)
            .alarmType(requestDto.getAlarmType())
            .url(requestDto.getUrl())
            .createdAt(LocalDateTime.now())
            .build();

        alarmRepository.save(alarm);
    }
}
