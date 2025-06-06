package icet.koco.posts.controller;

import icet.koco.enums.ApiResponseCode;
import icet.koco.global.dto.ApiResponse;
import icet.koco.posts.dto.PostCreateRequestDto;
import icet.koco.posts.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/backend/v3/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "게시판 관련 API들 입니다.")
public class PostController {
    private final PostService postService;

    @PostMapping
    @Operation(summary = "게시글을 등록하는 API입니다.")
    public ResponseEntity<?> createPost(@RequestBody PostCreateRequestDto requestDto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Long postId = postService.createPost(userId, requestDto);

        return ResponseEntity.ok(ApiResponse.success(ApiResponseCode.POST_CREATED, "게시물 등록에 성공했습니다.", postId));
    }

}
