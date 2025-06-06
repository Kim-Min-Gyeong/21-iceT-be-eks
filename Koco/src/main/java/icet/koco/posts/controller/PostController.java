package icet.koco.posts.controller;

import static icet.koco.enums.ApiResponseCode.POST_DETAIL_SUCCESS;

import icet.koco.enums.ApiResponseCode;
import icet.koco.global.dto.ApiResponse;
import icet.koco.posts.dto.PostCreateRequestDto;
import icet.koco.posts.dto.PostCreateResponseDto;
import icet.koco.posts.dto.PostGetDetailResponseDto;
import icet.koco.posts.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

        PostCreateResponseDto responseDto = postService.createPost(userId, requestDto);

        return ResponseEntity.ok(ApiResponse.success(ApiResponseCode.POST_CREATED, "게시물 등록에 성공했습니다.", responseDto));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostDetail(@PathVariable Long postId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        PostGetDetailResponseDto responseDto = postService.getPost(postId);

        return ResponseEntity.ok(ApiResponse.success(POST_DETAIL_SUCCESS, "게시물 상세 조회 성공", responseDto));
    }

}
