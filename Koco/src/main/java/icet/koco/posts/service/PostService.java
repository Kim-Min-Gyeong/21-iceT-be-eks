package icet.koco.posts.service;

import static java.time.LocalDateTime.now;

import icet.koco.global.exception.ForbiddenException;
import icet.koco.posts.dto.PostCreateRequestDto;
import icet.koco.posts.dto.PostCreateResponseDto;
import icet.koco.posts.dto.PostGetDetailResponseDto;
import icet.koco.posts.entity.Post;
import icet.koco.posts.entity.PostCategory;
import icet.koco.posts.repository.CommentRepository;
import icet.koco.posts.repository.LikeRepository;
import icet.koco.posts.repository.PostRepository;
import icet.koco.problemSet.entity.Category;
import icet.koco.problemSet.repository.CategoryRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;

    /**
     * 게시물 등록
     * @param userId 유저Id
     * @param requestDto (number, title, content, category)
     * @return postId
     */
    @Transactional
    public PostCreateResponseDto createPost(Long userId, PostCreateRequestDto requestDto) {
        // 유저 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("존재하지 않는 사용자입니다."));

        // Post Entity 생성
        Post post = Post.builder()
            .user(user)
            .problemNumber(requestDto.getProblemNumber())
            .commentCount(0)
            .likeCount(0)
            .content(requestDto.getContent())
            .title(requestDto.getTitle())
            .createdAt(now())
            .build();

        // 카테고리 이름으로 Category 조회 
        List<Category> categories = categoryRepository.findByNameIn(requestDto.getCategory());

        if (categories.size() != requestDto.getCategory().size()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리가 포함되어 있습니다.");
        }

        // 중간테이블에 카테고리 매핑
        for (Category category : categories) {
            PostCategory postCategory = PostCategory.builder()
                .category(category)
                .build();
            post.addPostCategory(postCategory);
        }

        // 저장
        postRepository.save(post);

        // DTO에 담아 반환
        return PostCreateResponseDto.builder()
            .postId(post.getId())
            .build();
    }

    /**
     * 게시글 상세 조회
     * @param postId 게시글 Id
     * @return PostGetDetailResponseDto
     */
    @Transactional(readOnly = true)
    public PostGetDetailResponseDto getPost(Long postId) {
        // 게시글 찾기
        Post post = postRepository.findByIdWithUserAndCategories(postId)
            .orElseThrow(() -> new ForbiddenException("해당 게시글이 존재하지 않습니다."));

        // 댓글, 좋아요 수
        Integer likeCount = likeRepository.countByPostId(postId);
        Integer commentCount = commentRepository.countByPostId(postId);

        // DTO에 맞춰서 반환
        return PostGetDetailResponseDto.builder()
            .postId(postId)
            .title(post.getTitle())
            .categories(
                post.getPostCategories().stream()
                    .map(c -> new PostGetDetailResponseDto.CategoryDto(c.getCategory().getId(), c.getCategory().getName()))
                    .toList()
            )
            .content(post.getContent())
            .author(PostGetDetailResponseDto.AuthorDto.builder()
                .userId(post.getUser().getId())
                .nickname(post.getUser().getNickname())
                .imgUrl(post.getUser().getProfileImgUrl())
                .build())
            .commentCount(commentCount)
            .likeCount(likeCount)
            .build();
    }
}
