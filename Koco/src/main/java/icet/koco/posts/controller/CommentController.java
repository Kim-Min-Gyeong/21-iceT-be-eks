package icet.koco.posts.controller;


import icet.koco.enums.ApiResponseCode;
import icet.koco.global.dto.ApiResponse;
import icet.koco.posts.dto.comment.CommentCreateRequestDto;
import icet.koco.posts.dto.comment.CommentCreateResponseDto;
import icet.koco.posts.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RequestMapping("/api/backend/v3/posts")
@Controller
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API 입니다.")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}/comments")
    @Operation(summary = "댓글 등록하는 API입니다.")
    public ResponseEntity<?> createComment(@PathVariable Long postId, @RequestBody CommentCreateRequestDto requestDto) {
        log.info("댓글 등록 controller");
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        CommentCreateResponseDto responseDto = commentService.createComment(userId, postId, requestDto);

        return ResponseEntity.ok(ApiResponse.success(ApiResponseCode.COMMENT_CREATED, "댓글이 성공적으로 등록되었습니다.", responseDto));
    }

}
