package icet.koco.alarm.controller;

import icet.koco.alarm.dto.AlarmListDto;
import icet.koco.alarm.dto.AlarmRequestDto;
import icet.koco.alarm.emitter.EmitterRepository;
import icet.koco.alarm.entity.Alarm;
import icet.koco.alarm.service.AlarmService;
import java.io.IOException;
import java.util.List;
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

        return alarmService.subscribe(userId, lastEventId);
    }


}
