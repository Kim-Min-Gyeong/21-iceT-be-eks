package icet.koco.auth.service;

import icet.koco.auth.dto.AuthResponse;
import icet.koco.auth.dto.KakaoUserResponse;
import icet.koco.auth.entity.OAuth;
import icet.koco.auth.repository.OAuthRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import icet.koco.util.JwtTokenProvider;
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
}