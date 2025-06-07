package icet.koco.posts.service;

import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.posts.dto.comment.CommentCreateRequestDto;
import icet.koco.posts.dto.comment.CommentCreateResponseDto;
import icet.koco.posts.entity.Comment;
import icet.koco.posts.entity.Post;
import icet.koco.posts.repository.CommentRepository;
import icet.koco.posts.repository.PostRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentCreateResponseDto createComment(Long userId, Long postId, CommentCreateRequestDto requestDto) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다."));

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));

        Comment comment = Comment.builder()
            .user(user)
            .post(post)
            .comment(requestDto.getContent())
            .createdAt(LocalDateTime.now())
            .build();

        commentRepository.save(comment);
        postRepository.incrementCommentCount(postId);

        return CommentCreateResponseDto.builder()
            .commentId(comment.getId())
            .build();
    }

}
