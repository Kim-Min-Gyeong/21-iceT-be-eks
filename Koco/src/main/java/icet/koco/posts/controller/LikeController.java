package icet.koco.posts.controller;

import icet.koco.enums.ApiResponseCode;
import icet.koco.global.dto.ApiResponse;
import icet.koco.posts.dto.like.LikeResponseDto;
import icet.koco.posts.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backend/v3/posts")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/{postId}/likes")
    public ResponseEntity<?> createLike(@PathVariable Long postId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        LikeResponseDto responseDto = likeService.createLike(userId, postId);

        return ResponseEntity.ok(ApiResponse.success(ApiResponseCode.LIKE_SUCCESS, "좋아요가 성공적으로 등록되었습니다.", responseDto));
    }

}
