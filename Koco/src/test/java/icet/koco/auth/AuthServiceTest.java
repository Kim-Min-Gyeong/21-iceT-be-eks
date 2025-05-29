package icet.koco.auth;

import icet.koco.auth.dto.AuthResponse;
import icet.koco.auth.service.AuthService;
import icet.koco.auth.service.KakaoOAuthClient;
import icet.koco.auth.dto.LogoutResponse;
import icet.koco.auth.dto.RefreshResponse;
import icet.koco.auth.dto.KakaoUserResponse;
import icet.koco.auth.entity.OAuth;
import icet.koco.auth.repository.OAuthRepository;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import icet.koco.util.CookieUtil;
import icet.koco.util.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthRepository oauthRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ValueOperations<String, String> valueOperations;


    @Test
    void 가입된_유저_카카오_로그인() {
        // given
        String code = "testCode";
        var kakaoUser = KakaoUserResponse.builder()
                .id(1234L)
                .kakao_account(KakaoUserResponse.KakaoAccount.builder()
                        .email("test@example.com")
                        .profile(KakaoUserResponse.KakaoAccount.Profile.builder()
                                .nickname("Test User")
                                .profile_image_url("http://example.com/profile.jpg")
                                .build())
                        .build())
                .build();

        var user = User.builder().id(1L).email("test@example.com").name("Test User").build();

        // Mock 연결
        given(kakaoOAuthClient.getUserInfo(code)).willReturn(kakaoUser);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(user)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(user)).willReturn("refresh-token");

        // Redis mock 연결
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When
        AuthResponse authResponse = authService.loginWithKakao(code, response);

        assertThat(authResponse.getCode()).isEqualTo("LOGIN_SUCCESS");
        assertThat(authResponse.getData().getEmail()).isEqualTo("test@example.com");

        // Then
        verify(valueOperations).set(eq("1"), eq("refresh-token"));
        verify(oauthRepository).updateRefreshToken(eq(1L), eq("refresh-token"));

    }
}

