package icet.koco.user.service;

import icet.koco.auth.entity.OAuth;
import icet.koco.auth.repository.OAuthRepository;
import icet.koco.auth.service.KakaoOAuthClient;
import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import icet.koco.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
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
}

