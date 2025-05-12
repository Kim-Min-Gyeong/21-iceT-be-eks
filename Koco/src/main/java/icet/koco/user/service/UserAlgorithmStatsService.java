package icet.koco.user.service;

import icet.koco.problemSet.entity.Category;
import icet.koco.problemSet.entity.Survey;
import icet.koco.problemSet.repository.CategoryRepository;
import icet.koco.problemSet.repository.ProblemCategoryRepository;
import icet.koco.problemSet.repository.SurveyRepository;
import icet.koco.user.entity.User;
import icet.koco.user.entity.UserAlgorithmStats;
import icet.koco.user.repository.UserAlgorithmStatsRepository;
import icet.koco.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAlgorithmStatsService {

    private final UserAlgorithmStatsRepository statsRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserAlgorithmStatsRepository userAlgorithmStatsRepository;
    private final SurveyRepository surveyRepository;
    private final ProblemCategoryRepository problemCategoryRepository;


    @Transactional
    public void upsertCorrectRate(Long userId, Long categoryId, Double correctRate) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));

        Optional<UserAlgorithmStats> existing = statsRepository.findByUserAndCategory(user, category);

        if (existing.isPresent()) {
            UserAlgorithmStats stats = existing.get();
            stats.setCorrectRate(correctRate.intValue());
        } else {
            UserAlgorithmStats newStats = UserAlgorithmStats.builder()
                .user(user)
                .category(category)
                .correctRate(correctRate.intValue())
                .build();
            statsRepository.save(newStats);
        }
    }

    @Transactional
    public void updateStatsFromSurveys(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 기존 통계 삭제
        userAlgorithmStatsRepository.deleteByUserId(userId);

        // 해당 유저의 설문 목록 조회
        List<Survey> surveys = surveyRepository.findByUserId(userId);

        if (surveys.isEmpty()) {
            log.info(">>>>> 유저 {} 의 설문 데이터 없음, 통계 생략", userId);
            return;
        }

        // 문제 ID 별로 정답 여부 저장
        Map<Long, Boolean> problemSolvedMap = new HashMap<>();
        for (Survey survey : surveys) {
            problemSolvedMap.put(survey.getProblem().getId(), survey.isSolved());
        }

        // 문제 ID -> 카테고리 ID 리스트 매핑
        Map<Long, List<Long>> problemToCategoryMap = problemCategoryRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                pc -> pc.getProblem().getId(),
                Collectors.mapping(pc -> pc.getCategory().getId(), Collectors.toList())
            ));

        // 카테고리별 맞은 문제 수 집계
        Map<Long, Integer> categoryCorrectCount = new HashMap<>();
        int totalCorrect = 0;

        for (Map.Entry<Long, Boolean> entry : problemSolvedMap.entrySet()) {
            Long problemId = entry.getKey();
            Boolean isSolved = entry.getValue();

            if (!Boolean.TRUE.equals(isSolved)) continue;

            List<Long> categoryIds = problemToCategoryMap.getOrDefault(problemId, List.of());
            for (Long categoryId : categoryIds) {
                categoryCorrectCount.put(categoryId, categoryCorrectCount.getOrDefault(categoryId, 0) + 1);
                totalCorrect++;
            }
        }

        // 통계 저장
        List<UserAlgorithmStats> statsToSave = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : categoryCorrectCount.entrySet()) {
            Long categoryId = entry.getKey();
            int correct = entry.getValue();
            double correctRatio = totalCorrect > 0 ? (correct * 100.0 / totalCorrect) : 0.0;

            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리를 찾을 수 없습니다. " + categoryId));

            UserAlgorithmStats stats = UserAlgorithmStats.builder()
                .user(user)
                .category(category)
                .correctRate((int) Math.round(correctRatio)) // 비중 기반
                .build();

            statsToSave.add(stats);
        }

        userAlgorithmStatsRepository.saveAll(statsToSave);
        log.info(">>>>> 유저 {} 알고리즘 통계 {}건 저장 완료", userId, statsToSave.size());
    }

}
