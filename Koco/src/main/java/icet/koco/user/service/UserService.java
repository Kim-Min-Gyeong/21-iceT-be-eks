package icet.koco.user.service;

import icet.koco.auth.entity.OAuth;
import icet.koco.auth.repository.OAuthRepository;
import icet.koco.auth.service.KakaoOAuthClient;
import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.problemSet.entity.ProblemSet;
import icet.koco.problemSet.repository.ProblemSetRepository;
import icet.koco.problemSet.repository.SolutionRepository;
import icet.koco.problemSet.repository.SurveyRepository;
import icet.koco.user.dto.DashboardResponseDto;
import icet.koco.user.dto.UserCategoryStatProjection;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import icet.koco.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final OAuthRepository oAuthRepository;
    private final CookieUtil cookieUtil;
    private final ProblemSetRepository problemSetRepository;
    private final SurveyRepository surveyRepository;

    @Transactional
    public void deleteUser(Long userId, HttpServletResponse response) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("해당 유저 정보를 찾을 수 없습니다."));

        // 2. OAuth 정보 조회
        OAuth oauth = oAuthRepository.findByUserId(userId)
            .orElseThrow(() -> new UnauthorizedException("OAuth 정보가 존재하지 않습니다."));

        // 3. Kakao unlink 호출
        try {
            kakaoOAuthClient.unlinkUser(oauth.getProviderId());
        } catch (Exception e) {
            log.warn(">>>>> Kakao unlink 실패: {}", e.getMessage());
        }

        // 4. Redis refreshToken 삭제
        redisTemplate.delete(userId.toString());

        // 5. Soft delete 처리
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        // 6. 쿠키 삭제 처리
        cookieUtil.invalidateCookie(response, "access_token");
        cookieUtil.invalidateCookie(response, "refresh_token");
    }

    @Transactional
    public void updateUserInfo(Long userId, String nickname, String profileImgUrl, String statusMsg) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        if (nickname != null) user.setNickname(nickname);
        if (profileImgUrl != null) user.setProfileImgUrl(profileImgUrl);
        if (statusMsg != null) user.setStatusMsg(statusMsg);
    }

    // 대시볻 조회 API
    @Transactional(readOnly = true)
    public DashboardResponseDto getUserDashboard(Long userId, LocalDate date) {
        User user = userRepository.findById(userId).orElseThrow();
        log.info("userId: {}", user.getId());
        Long problemSetId = problemSetRepository.findByCreatedAt(date).map(ProblemSet::getId).orElse(null);

        // 상위 5개의 알고리즘 분야 가져오기
        List<UserCategoryStatProjection> projections = surveyRepository.calculateCorrectRateByCategory(userId);
        List<DashboardResponseDto.CategoryStat> statDtos = projections.stream()
            .limit(5)
            .map(p -> DashboardResponseDto.CategoryStat.builder()
                .categoryId(p.getCategoryId())
                .categoryName(p.getCategoryName())
                .correctRate(Math.round(p.getCorrectRate() * 1000) / 10.0)
                .build())
            .toList();

        return DashboardResponseDto.builder()
            .userId(user.getId())
            .nickname(user.getNickname())
            .statusMessage(user.getStatusMsg())
            .profileImgUrl(user.getProfileImgUrl())
            .todayProblemSetId(problemSetId)
            .studyStats(statDtos)
            .build();
    }

}

