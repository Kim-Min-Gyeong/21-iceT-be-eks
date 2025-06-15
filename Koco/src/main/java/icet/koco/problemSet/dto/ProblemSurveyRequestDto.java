package icet.koco.problemSet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemSurveyRequestDto {
    private Long problemId;
    private boolean isSolved;
    private String difficultyLevel;
}