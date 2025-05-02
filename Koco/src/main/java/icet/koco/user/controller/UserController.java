package icet.koco.user.controller;

import icet.koco.global.dto.ApiResponse;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.user.service.UserService;
import icet.koco.util.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<?>> deleteUser() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println(">>>>> Controller userId: " + userId);

        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
ㄴ
}
