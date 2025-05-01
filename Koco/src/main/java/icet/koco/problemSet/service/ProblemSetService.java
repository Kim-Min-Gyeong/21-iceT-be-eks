package icet.koco.problemSet.service;

import icet.koco.global.exception.ForbiddenException;
import icet.koco.problemSet.dto.ProblemSetResponseDto;
import icet.koco.problemSet.dto.ProblemDto;
import icet.koco.problemSet.entity.Problem;
import icet.koco.problemSet.entity.ProblemSet;
import icet.koco.problemSet.entity.ProblemSetProblem;
import icet.koco.problemSet.repository.ProblemRepository;
import icet.koco.problemSet.repository.ProblemSetProblemRepository;
import icet.koco.problemSet.repository.ProblemSetRepository;
import icet.koco.problemSet.repository.SurveyRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ProblemSetService {

    private final ProblemSetRepository problemSetRepository;
    private final ProblemSetProblemRepository problemSetProblemRepository;
    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public ProblemSetResponseDto getProblemSetByDate(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("사용자를 찾을 수 없습니다."));

        ProblemSet problemSet = problemSetRepository.findByCreatedAt(date)
            .orElseThrow(() -> new ForbiddenException("해당 날짜에 출제된 문제집을 찾을 수 없습니다."));

        // ⭐ 중간 테이블 통해 문제 리스트 조회
        List<ProblemSetProblem> mappings = problemSetProblemRepository.findAllByProblemSet(problemSet);

        List<Problem> problems = mappings.stream()
            .map(ProblemSetProblem::getProblem)
            .toList();

        List<Long> problemIds = problems.stream()
            .map(Problem::getId)
            .toList();

        long answeredCount = surveyRepository.countByUserAndProblemSet(user, problemSet);
        List<Long> answeredProblemIds = surveyRepository.findProblemIdsByUserAndProblemSet(userId, problemSet.getId());

        boolean isAnswered = (answeredCount == problems.size()) && answeredProblemIds.containsAll(problemIds);

        List<ProblemDto> problemDtos = problems.stream()
            .map(ProblemDto::from)
            .toList();

        return ProblemSetResponseDto.builder()
            .date(problemSet.getCreatedAt())
            .problemSetId(problemSet.getId())
            .isAnswered(isAnswered)
            .problems(problemDtos)
            .build();
    }
}
