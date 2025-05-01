package icet.koco.problemSet.service;

import icet.koco.global.exception.ForbiddenException;
import icet.koco.problemSet.dto.ProblemSetResponseDto;
import icet.koco.problemSet.dto.ProblemDto;
import icet.koco.problemSet.entity.ProblemSet;
import icet.koco.problemSet.entity.ProblemSetProblem;
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

    @Transactional(readOnly = true)
    public ProblemSetResponseDto getProblemSetByDate(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("사용자를 찾을 수 없습니다."));

        ProblemSet problemSet = problemSetRepository.findByCreatedAt(date)
            .orElseThrow(() -> new ForbiddenException("해당 날짜에 출제된 문제집을 찾을 수 없습니다."));

        List<ProblemSetProblem> mappings = problemSetProblemRepository.findAllByProblemSet(problemSet);
        List<ProblemDto> problemDtos = mappings.stream()
            .map(mapping -> ProblemDto.from(mapping.getProblem()))
            .toList();

        boolean isAnswered = surveyRepository.existsByUserAndProblemSet(user, problemSet);

        return ProblemSetResponseDto.builder()
            .date(problemSet.getCreatedAt())
            .problemSetId(problemSet.getId())
            .isAnswered(isAnswered)
            .problems(problemDtos)
            .build();
    }
}
