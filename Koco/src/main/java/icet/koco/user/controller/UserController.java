package icet.koco.user.controller;

import icet.koco.global.dto.ApiResponse;
import icet.koco.user.dto.DashboardResponseDto;
import icet.koco.user.dto.UserResponse;
import icet.koco.user.service.UserService;
import icet.koco.user.service.uploader.ImageUploader;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@RestController
@RequestMapping("/api/backend/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ImageUploader imageUploader;

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<?>> deleteUser(HttpServletResponse response) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.deleteUser(userId, response);

        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateUserInfo(
        @RequestPart(value = "nickname", required = false) String nickname,
        @RequestPart(value = "statusMsg", required = false) String statusMsg,
        @RequestPart(value = "profileImg", required = false) MultipartFile profileImg) {

        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String profileImgUrl = (profileImg != null && !profileImg.isEmpty()) ? imageUploader.upload(profileImg) : null;

        userService.updateUserInfo(userId, nickname, profileImgUrl, statusMsg);

        return ResponseEntity.ok(UserResponse.ofSuccess());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            Long userId = Long.valueOf(principal.toString());
            DashboardResponseDto response = userService.getUserDashboard(userId, date);
            return ResponseEntity.ok(ApiResponse.success("USER_DASHBOARD_GET_SUCCESS", "유저 프로필 정보 조회 성공", response));
        } catch (Exception e) {
            log.error("대시보드 API 에러 발생", e);
            e.printStackTrace(); // 콘솔에 전체 에러 출력
            return ResponseEntity.internalServerError().body(ApiResponse.fail("INTERNAL_SERVER_ERROR", "셔벼 내부 에러"));
        }
    }

}
