package icet.koco.alarm.controller;

import icet.koco.alarm.dto.AlarmRequestDto;
import icet.koco.alarm.service.AlarmService;
import icet.koco.enums.ApiResponseCode;
import icet.koco.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backend/v3/alarms")
@RequiredArgsConstructor
public class AlarmController {
    private final AlarmService alarmService;

    public ResponseEntity<?> createAlarm(@RequestBody AlarmRequestDto requestDto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        alarmService.createAlarm(userId, requestDto);

        return ResponseEntity.noContent().build();

    }


}
