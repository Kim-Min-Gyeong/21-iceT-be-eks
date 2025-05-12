package icet.koco.user.service;

import icet.koco.auth.entity.OAuth;
import icet.koco.auth.repository.OAuthRepository;
import icet.koco.auth.service.KakaoOAuthClient;
import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.problemSet.repository.ProblemSetRepository;
import icet.koco.problemSet.repository.SurveyRepository;
import icet.koco.user.dto.UserAlgorithmStatsResponseDto;
import icet.koco.user.dto.UserCategoryStatProjection;
import icet.koco.user.dto.UserInfoResponseDto;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserAlgorithmStatsRepository;
import icet.koco.user.repository.UserRepository;
import icet.koco.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
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
    private final UserAlgorithmStatsRepository userAlgorithmStatsRepository;
    private final UserAlgorithmStatsService userAlgorithmStatsService;


    /**
     * 유저 탈퇴
     * @param userId
     * @param response
     */
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

    /**
     * 유저 정보 수정(업데이트)
     * @param userId
     * @param nickname
     * @param profileImgUrl
     * @param statusMsg
     */
    @Transactional
    public void updateUserInfo(Long userId, String nickname, String profileImgUrl, String statusMsg) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        if (nickname != null) user.setNickname(nickname);
        if (profileImgUrl != null) user.setProfileImgUrl(profileImgUrl);
        if (statusMsg != null) user.setStatusMsg(statusMsg);
    }

    /**
     * 유저 정보 조회
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public UserInfoResponseDto getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        return UserInfoResponseDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .statusMessage(user.getStatusMsg())
                .profileImageUrl(user.getProfileImgUrl())
                .build();
    }

    // userAlgorithmStats 조회 API
    @Transactional
    public UserAlgorithmStatsResponseDto getAlgorithmStats(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        log.info("userId: {}", user.getId());

        List<UserCategoryStatProjection> stats = surveyRepository.calculateCorrectRateByCategory(userId);

        for (UserCategoryStatProjection stat : stats) {
            userAlgorithmStatsService.upsertCorrectRate(
                userId,
                stat.getCategoryId(),
                Math.round(stat.getCorrectRate() * 1000) / 10.0 // 소수 첫째 자리까지 반올림
            );
        }

        List<UserAlgorithmStatsResponseDto.CategoryStat> statDtos = stats.stream()
            .limit(5)
            .map(p -> UserAlgorithmStatsResponseDto.CategoryStat.builder()
                .categoryId(p.getCategoryId())
                .categoryName(p.getCategoryName())
                .correctRate(Math.round(p.getCorrectRate() * 1000) / 10.0)
                .build())
            .toList();

        return UserAlgorithmStatsResponseDto.builder()
            .studyStats(statDtos)
            .build();
    }
}

