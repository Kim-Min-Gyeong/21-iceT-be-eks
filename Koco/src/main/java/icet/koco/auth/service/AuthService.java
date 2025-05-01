package icet.koco.auth.service;

import icet.koco.auth.dto.AuthResponse;
import icet.koco.auth.dto.KakaoUserResponse;
import icet.koco.auth.dto.RefreshResponse;
import icet.koco.auth.entity.OAuth;
import icet.koco.auth.repository.OAuthRepository;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import icet.koco.util.JwtTokenProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final OAuthRepository oauthRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthResponse loginWithKakao(String code, HttpServletResponse response) {
        KakaoUserResponse kakaoUser = kakaoOAuthClient.getUserInfo(code);
        Optional<User> userOpt = userRepository.findByEmail(kakaoUser.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // RefreshToken Redis에 저장
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            redisTemplate.opsForValue().set(user.getId().toString(), refreshToken);

            oauthRepository.updateRefreshToken(user.getId(), refreshToken);
            String accessToken = jwtTokenProvider.createAccessToken(user);

            // Token(access, refresh) 발급 확인용
            System.out.println(">>>>> (AuthService: loginWithKakao) Access token: " + accessToken);
            System.out.println(">>>>> (AuthService: loginWithKakao) Refresh token: " + refreshToken);
            Cookie cookie = new Cookie("accessToken", accessToken);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(30 * 60); // 30분 (JWT 만료와 맞춤)
            cookie.setPath("/");
            response.addCookie(cookie);

            return AuthResponse.builder()
                .code("LOGIN_SUCCESS")
                .message("로그인 성공. 토큰 발급 완료")
                .data(AuthResponse.AuthData.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .refreshToken(refreshToken)
                    .isRegistered(true)
                    .build())
                .build();
        }

        // 신규 회원
        User newUser = userRepository.save(User.builder()
            .email(kakaoUser.getEmail())
            .name(kakaoUser.getName())
            .createdAt(LocalDateTime.now())
            .build());

        String refreshToken = jwtTokenProvider.createRefreshToken(newUser);
        oauthRepository.save(OAuth.builder()
            .provider("kakao")
            .providerId(kakaoUser.getProviderId())
            .user(newUser)
            .refreshToken(refreshToken)
            .build());

        Cookie cookie = new Cookie("accessToken", jwtTokenProvider.createAccessToken(newUser));
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        return AuthResponse.builder()
            .code("LOGIN_SUCCESS")
            .message("로그인 성공. 토큰 발급 완료")
            .data(AuthResponse.AuthData.builder()
                .email(newUser.getEmail())
                .name(newUser.getName())
                .refreshToken(refreshToken)
                .isRegistered(false)
                .build())
            .build();
    }

    // 리프레쉬 토큰 재발급
    public RefreshResponse refreshAccessToken(String refreshToken, HttpServletResponse response) {
        // 1. 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. 토큰에서 userId 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Optional<OAuth> oauthOpt = oauthRepository.findByUserId(userId);

        // 3. Redis 또는 DB에서 저장된 리프레시 토큰 가져오기
        String redisToken = redisTemplate.opsForValue().get(userId.toString());
        if (redisToken == null || !redisToken.equals(refreshToken)) {
            throw new UnauthorizedException("Redis에 저장된 토큰이 일치하지 않습니다.");
        } else {
            if (oauthOpt.isEmpty() || !refreshToken.equals(oauthOpt.get().getRefreshToken())) {
                throw new UnauthorizedException("토큰이 일치하지 않습니다.");
            }
        }

        // 4. 새 accessToken 생성 및 쿠키 저장
        User user = oauthOpt.get().getUser();
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        System.out.println(">>>>> (AuthService: refreshAccessToken) New Access token: " + newAccessToken);

        Cookie cookie = new Cookie("access_token", newAccessToken);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(30 * 60); // 30분 (JWT 만료와 맞춤)
        cookie.setPath("/");
        response.addCookie(cookie);

        // 5. 응답 생성
        return RefreshResponse.builder()
            .code("TOKEN_REFRESH_SUCCESS")
            .message("토큰이 성공적으로 재발급되었습니다.")
            .data(Map.of("accessToken", newAccessToken))
            .build();
    }

}