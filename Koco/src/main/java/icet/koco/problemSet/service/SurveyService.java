package icet.koco.problemSet.service;

import icet.koco.enums.DifficultyLevel;
import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.problemSet.dto.ProblemSetSurveyRequestDto;
import icet.koco.problemSet.dto.ProblemSurveyRequestDto;
import icet.koco.problemSet.dto.SurveyResponseDto;
import icet.koco.problemSet.entity.Problem;
import icet.koco.problemSet.entity.ProblemSet;
import icet.koco.problemSet.entity.Survey;
import icet.koco.problemSet.repository.ProblemRepository;
import icet.koco.problemSet.repository.ProblemSetRepository;
import icet.koco.problemSet.repository.SurveyRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final UserRepository userRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemRepository problemRepository;
    private final SurveyRepository surveyRepository;

    @Transactional
    public SurveyResponseDto submitSurvey(Long userId, ProblemSetSurveyRequestDto requestDto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("존재하지 않는 사용자입니다."));

        ProblemSet problemSet = problemSetRepository.findById(requestDto.getProblemSetId())
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 문제집입니다."));

        List<Long> savedIds = new ArrayList<>();

        for (ProblemSurveyRequestDto response : requestDto.getResponses()) {
            Problem problem = problemRepository.findById(response.getProblemId())
                .orElseThrow(() -> new ResourceNotFoundException("문제 ID " +response.getProblemId() + "가 존재하지 않습니다."));

            Survey survey = Survey.builder()
                .user(user)
                .problemSet(problemSet)
                .problem(problem)
                .isSolved(response.isSolved())
//                .isSolved(response.getIsSolved())
                .difficultyLevel(DifficultyLevel.valueOf(response.getDifficultyLevel()))
                .answeredAt(LocalDateTime.now())
                .build();

            surveyRepository.save(survey);
            savedIds.add(survey.getId());
        }

        return SurveyResponseDto.builder()
            .code("SURVEY_CREATED")
            .message("출제 문제집에 대한 설문응답이 성공적으로 생성되었습니다.")
            .data(Map.of("surveyId", savedIds))
            .build();
    }
}