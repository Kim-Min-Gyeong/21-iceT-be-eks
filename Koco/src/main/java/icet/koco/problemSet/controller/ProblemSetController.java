package icet.koco.problemSet.controller;

import icet.koco.global.dto.ApiResponse;
import icet.koco.problemSet.dto.ProblemSetResponseDto;
import icet.koco.problemSet.service.ProblemSetService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/problem-set")
public class ProblemSetController {

    private final ProblemSetService problemSetService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProblemSetResponseDto>> getProblemSetByDate(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        ProblemSetResponseDto response = problemSetService.getProblemSetByDate(userId, date);
        return ResponseEntity.ok(
            ApiResponse.success("PROBLEM_SET_FETCH_SUCCESS", "문제 리스트 조회 성공", response)
        );
    }
}
