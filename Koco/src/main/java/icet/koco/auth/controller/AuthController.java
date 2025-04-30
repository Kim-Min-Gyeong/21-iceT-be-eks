package icet.koco.auth.controller;

import icet.koco.auth.dto.*;
import icet.koco.auth.service.AuthService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @Operation(
        summary = "Kakao 로그인 callback",
        description = "인가 코드로 액세스 토큰 발급 후 사용자 정보 반환"
    )
    @CrossOrigin(origins = "*")
    @GetMapping("/callback")
    public ResponseEntity<AuthResponse> kakaoCallback(@RequestParam("code") String code,
        HttpServletResponse response) {
        System.out.println("인가코드: " + code);
        AuthResponse authResponse = authService.loginWithKakao(code, response);
        return ResponseEntity.ok(authResponse);


    }

}