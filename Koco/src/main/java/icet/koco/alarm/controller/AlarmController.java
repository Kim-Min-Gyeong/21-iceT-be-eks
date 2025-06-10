package icet.koco.alarm.controller;

import icet.koco.alarm.dto.AlarmRequestDto;
import icet.koco.alarm.emitter.EmitterRepository;
import icet.koco.alarm.service.AlarmService;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.yaml.snakeyaml.emitter.Emitter;

@RestController
@RequestMapping("/api/backend/v3/alarms")
@RequiredArgsConstructor
public class AlarmController {
    private final AlarmService alarmService;
    private final EmitterRepository emitterRepository;

    @PostMapping
    public ResponseEntity<?> createAlarmInternal(@RequestBody AlarmRequestDto requestDto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        alarmService.createAlarmInternal(requestDto);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String emitterId = makeEmitterId(userId);
        SseEmitter emitter = new SseEmitter(60L * 1000 * 60); // 1시간

        emitterRepository.save(emitterId, emitter);

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
        emitter.onError((e) -> emitterRepository.deleteById(emitterId));

        // 연결 응답 보내기
        try {
            emitter.send(SseEmitter.event()
                .id(emitterId)
                .name("connect")
                .data("connected"));
        } catch (IOException e) {
            throw new RuntimeException("SSE 연결 실패");
        }

        // 재전송 이벤트
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

        return emitter;

    }


    String makeEmitterId(Long userId) {
        return userId + "_" + System.currentTimeMillis();
    }
}
