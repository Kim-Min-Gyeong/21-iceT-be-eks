package icet.koco.user.service;

import icet.koco.problemSet.entity.Category;
import icet.koco.problemSet.repository.CategoryRepository;
import icet.koco.user.entity.User;
import icet.koco.user.entity.UserAlgorithmStats;
import icet.koco.user.repository.UserAlgorithmStatsRepository;
import icet.koco.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAlgorithmStatsService {

    private final UserAlgorithmStatsRepository statsRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

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
}
