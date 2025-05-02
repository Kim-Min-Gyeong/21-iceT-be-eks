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
import icet.koco.problemSet.repository.ProblemSetProblemRepository;
import icet.koco.problemSet.repository.ProblemSetRepository;
import icet.koco.problemSet.repository.SurveyRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final UserRepository userRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemRepository problemRepository;
    private final SurveyRepository surveyRepository;
    private final ProblemSetProblemRepository problemSetProblemRepository;

    @Transactional
    public SurveyResponseDto submitSurvey(Long userId, ProblemSetSurveyRequestDto requestDto) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new UnauthorizedException("존재하지 않는 사용자입니다."));

        Long problemSetId = requestDto.getProblemSetId();
        ProblemSet problemSet = problemSetRepository.findById(problemSetId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 문제집입니다."));

        Set<Long> requestProblemIds = requestDto.getResponses().stream()
            .map(ProblemSurveyRequestDto::getProblemId)
            .collect(Collectors.toSet());

        Map<Long, Problem> problemMap = problemRepository.findAllById(requestProblemIds).stream()
            .collect(Collectors.toMap(Problem::getId, p -> p));

        // 포함된 문제 ID만 조회 (JPQL 기반)
        List<Long> includedIds = problemSetProblemRepository
            .findIncludedProblemIds(problemSetId, new ArrayList<>(requestProblemIds));
        Set<Long> includedProblemIds = new HashSet<>(includedIds);

        List<Survey> surveysToSave = new ArrayList<>();

        for (ProblemSurveyRequestDto response : requestDto.getResponses()) {
            Long pid = response.getProblemId();

            Problem problem = problemMap.get(pid);
            if (problem == null) {
                throw new ResourceNotFoundException("문제 ID " + pid + "가 존재하지 않습니다.");
            }
            if (!includedProblemIds.contains(pid)) {
                throw new UnauthorizedException("문제 ID " + pid + "는 문제집 " + problemSetId + "에 포함되어 있지 않습니다.");
            }

            Survey survey = Survey.builder()
                .user(user)
                .problemSet(problemSet)
                .problem(problem)
                .isSolved(response.isSolved())
                .difficultyLevel(DifficultyLevel.valueOf(response.getDifficultyLevel().toUpperCase()))
                .answeredAt(LocalDateTime.now())
                .build();

            surveysToSave.add(survey);
        }

        List<Survey> saved = surveyRepository.saveAll(surveysToSave);
        List<Long> ids = saved.stream().map(Survey::getId).toList();

        return SurveyResponseDto.builder()
            .code("SURVEY_CREATED")
            .message("출제 문제집에 대한 설문응답이 성공적으로 생성되었습니다.")
            .data(Map.of("surveyId", ids))
            .build();
    }
}
