package icet.koco.alarm.controller;

import icet.koco.alarm.dto.AlarmListResponseDto;
import icet.koco.alarm.dto.AlarmRequestDto;
import icet.koco.alarm.emitter.EmitterRepository;
import icet.koco.alarm.entity.Alarm;
import icet.koco.alarm.service.AlarmService;

import icet.koco.enums.ApiResponseCode;
import icet.koco.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping
    public ResponseEntity<?> getAlarms(
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false, defaultValue = "10") int size)  {

        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        AlarmListResponseDto responseDto = alarmService.getAlarmList(userId, cursorId, size);

        return ResponseEntity.ok(ApiResponse.success(ApiResponseCode.SUCCESS, "알림 리스트 조회에 성공하였습니다.", responseDto));
    }

}
