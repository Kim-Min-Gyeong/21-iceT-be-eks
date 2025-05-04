package icet.koco.user.controller;

import icet.koco.global.dto.ApiResponse;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.user.dto.UserResponse;
import icet.koco.user.service.UserService;
import icet.koco.user.service.uploader.ImageUploader;
import icet.koco.util.JwtTokenProvider;
import icet.koco.util.TokenExtractor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenExtractor tokenExtractor;
    private final ImageUploader imageUploader;

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

    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateUserInfo(
        @RequestPart(value = "nickname", required = false) String nickname,
        @RequestPart(value = "statusMsg", required = false) String statusMsg,
        @RequestPart(value = "profileImg", required = false) MultipartFile profileImg,
        @CookieValue("access_token") String accessToken) {

        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        String profileImgUrl = (profileImg != null && !profileImg.isEmpty()) ? imageUploader.upload(profileImg) : null;

        userService.updateUserInfo(userId, nickname, profileImgUrl, statusMsg);

        return ResponseEntity.ok(UserResponse.ofSuccess());
    }
}
