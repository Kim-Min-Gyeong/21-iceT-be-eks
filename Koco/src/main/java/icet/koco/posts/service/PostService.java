package icet.koco.posts.service;

import static java.time.LocalDateTime.now;

import icet.koco.global.exception.ForbiddenException;
import icet.koco.posts.dto.PostCreateRequestDto;
import icet.koco.posts.entity.Post;
import icet.koco.posts.entity.PostCategory;
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

    @Transactional
    public Long createPost(Long userId, PostCreateRequestDto requestDto) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("존재하지 않는 사용자입니다."));

        Post post = Post.builder()
            .user(user)
            .problemNumber(requestDto.getProblemNumber())
            .commentCount(0)
            .likeCount(0)
            .content(requestDto.getContent())
            .title(requestDto.getTitle())
            .createdAt(now())
            .build();

        List<Category> categories = categoryRepository.findByNameIn(requestDto.getCategory());

        if (categories.size() != requestDto.getCategory().size()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리가 포함되어 있습니다.");
        }

        for (Category category : categories) {
            PostCategory postCategory = PostCategory.builder()
                .category(category)
                .build();
            post.addPostCategory(postCategory);
        }

        return postRepository.save(post).getId();
    }
}
