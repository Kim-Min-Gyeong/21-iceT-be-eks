package icet.koco.problemSet.controller;


import icet.koco.global.dto.ApiResponse;
import icet.koco.problemSet.dto.AiSolutionRequestDto;
import icet.koco.problemSet.service.SolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/solution")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;

    @PostMapping
    public ResponseEntity<?> receiveSolution(@RequestBody AiSolutionRequestDto dto) {
        solutionService.saveFromAi(dto);
        return ResponseEntity.ok(ApiResponse.success("SOLUTION_RECEIVED", "해설 저장 완료", null));
    }
}