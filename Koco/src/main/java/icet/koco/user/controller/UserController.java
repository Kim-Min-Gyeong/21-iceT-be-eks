package icet.koco.user.controller;

import icet.koco.global.dto.ApiResponse;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.user.service.UserService;
import icet.koco.util.JwtTokenProvider;
import icet.koco.util.TokenExtractor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenExtractor tokenExtractor;

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<?>> deleteUser(HttpServletRequest request, HttpServletResponse response) {
        String token = tokenExtractor.extractFromCookies(request, "access_token");
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("유효하지 않거나 만료된 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        userService.deleteUser(userId, response);

        return ResponseEntity.noContent().build();
    }



}
