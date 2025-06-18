package icet.koco.post;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import icet.koco.enums.ErrorMessage;
import icet.koco.fixture.CategoryFixture;
import icet.koco.fixture.PostFixture;
import icet.koco.fixture.ProblemFixture;
import icet.koco.fixture.UserFixture;
import icet.koco.global.exception.BadRequestException;
import icet.koco.global.exception.ForbiddenException;
import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.posts.dto.post.PostCreateEditRequestDto;
import icet.koco.posts.dto.post.PostCreateResponseDto;
import icet.koco.posts.dto.post.PostGetDetailResponseDto;
import icet.koco.posts.entity.Post;
import icet.koco.posts.entity.PostCategory;
import icet.koco.problemSet.entity.Category;
import icet.koco.problemSet.entity.Problem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import icet.koco.posts.repository.CommentRepository;
import icet.koco.posts.repository.LikeRepository;
import icet.koco.posts.repository.PostCategoryRepository;
import icet.koco.posts.repository.PostRepository;
import icet.koco.posts.service.PostService;
import icet.koco.problemSet.entity.ProblemCategory;
import icet.koco.problemSet.repository.CategoryRepository;
import icet.koco.problemSet.repository.ProblemCategoryRepository;
import icet.koco.problemSet.repository.ProblemRepository;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private ProblemRepository problemRepository;

	@Mock
	private ProblemCategoryRepository problemCategoryRepository;

	@Mock
	private PostCategoryRepository postCategoryRepository;

	private static final Long USER_ID = 1L;
	private static final Long POST_ID = 100L;
	private static final Long VALID_PROBLEM_NUMBER = 30000L;
	private static final Long INVALID_PROBLEM_NUMBER = 9999L;
	private static final String CATEGORY_DP = "dp";
	private static final List<String> VALID_CATEGORIES = List.of(CATEGORY_DP);
	private static final List<String> INVALID_CATEGORIES = List.of("test", CATEGORY_DP);

    private User user;
    private Post post;
    private Category category;
    private Problem problem;
    private PostCreateEditRequestDto requestDto;

	@BeforeEach
	void setUp() {
		user = UserFixture.validUser();
		category = CategoryFixture.category(1L, CATEGORY_DP);
		problem = ProblemFixture.problem(1L, VALID_PROBLEM_NUMBER);
		post = PostFixture.postWithCategory(user, category, POST_ID, VALID_PROBLEM_NUMBER);
	}

    @Nested
    @DisplayName("게시글 생성")
    class createPostTest {
        @Test
        void createPost_성공() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(problemRepository.findByNumber(VALID_PROBLEM_NUMBER)).willReturn(Optional.of(problem));
            given(categoryRepository.findByNameIn(VALID_CATEGORIES)).willReturn(List.of(category));
			PostCreateEditRequestDto requestDto = PostFixture.requestDto(VALID_PROBLEM_NUMBER, VALID_CATEGORIES, PostFixture.TEST_TITLE, PostFixture.TEST_CONTENT);

            // when
            PostCreateResponseDto responseDto = postService.createPost(USER_ID, requestDto);

            // then
            assertThat(responseDto).isNotNull();

            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            then(postRepository).should().save(postCaptor.capture());

            Post savedPost = postCaptor.getValue();
            assertThat(savedPost.getTitle()).isEqualTo(PostFixture.TEST_TITLE);
            assertThat(savedPost.getContent()).isEqualTo(PostFixture.TEST_CONTENT);
            assertThat(savedPost.getUser()).isEqualTo(user);
            assertThat(savedPost.getProblemNumber()).isEqualTo(VALID_PROBLEM_NUMBER);
            assertThat(savedPost.getPostCategories().get(0).getCategory().getName()).isEqualTo("dp");
        }

        @Test
        void createPost_존재하지않는사용자() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.createPost(USER_ID, requestDto))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage(ErrorMessage.USER_NOT_FOUND.getMessage());

        }

        @Test
        void createPost_존재하지않는문제번호() {
			PostCreateEditRequestDto requestDto = PostFixture.requestDto(INVALID_PROBLEM_NUMBER, VALID_CATEGORIES, PostFixture.TEST_TITLE, PostFixture.TEST_CONTENT);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(problemRepository.findByNumber(INVALID_PROBLEM_NUMBER)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.createPost(USER_ID, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessage.INVALID_PROBLEM_INCLUDED.getMessage());
        }

        @Test
        void createPost_존재하지않는카테고리() {
			PostCreateEditRequestDto requestDto = PostFixture.requestDto(VALID_PROBLEM_NUMBER, INVALID_CATEGORIES, PostFixture.TEST_TITLE, PostFixture.TEST_CONTENT);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(problemRepository.findByNumber(VALID_PROBLEM_NUMBER)).willReturn(Optional.of(problem));
			given(categoryRepository.findByNameIn(INVALID_CATEGORIES)).willReturn(List.of(category));

			// when & then
            assertThatThrownBy(() -> postService.createPost(USER_ID, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessage.INVALID_PROBLEM_INCLUDED.getMessage());
        }
    }

    @Nested
    @DisplayName("게시글 조회")
    class getPostTest {

        @Test
        void getPost_성공() {
            // given
            given(postRepository.findByIdWithUserAndCategories(POST_ID)).willReturn(Optional.of(post));
            given(likeRepository.countByPostId(POST_ID)).willReturn(5);
            given(commentRepository.countByPostIdAndDeletedAtIsNull(POST_ID)).willReturn(5);
            given(likeRepository.existsByUserIdAndPostId(USER_ID, POST_ID)).willReturn(false);

            // when
            PostGetDetailResponseDto responseDto = postService.getPost(USER_ID, POST_ID);

            // then
            assertThat(responseDto).isNotNull();
            assertThat(responseDto.getPostId()).isEqualTo(POST_ID);
            assertThat(responseDto.getTitle()).isEqualTo(PostFixture.TEST_TITLE);
            assertThat(responseDto.getContent()).isEqualTo(PostFixture.TEST_CONTENT);
            assertThat(responseDto.getLikeCount()).isEqualTo(5);
            assertThat(responseDto.getCommentCount()).isEqualTo(5);
            assertThat(responseDto.isLiked()).isFalse();
            assertThat(responseDto.getCategories().size()).isEqualTo(1);
			assertThat(responseDto.getCategories().get(0).getCategoryName()).isEqualTo(CATEGORY_DP);
			assertThat(responseDto.getAuthor().getNickname()).isEqualTo(user.getNickname());
			assertThat(responseDto.getAuthor().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        void getPost_존재하지않는게시글() {
            // given
            given(postRepository.findByIdWithUserAndCategories(POST_ID)).willReturn(Optional.empty());

            // then & when
            assertThatThrownBy(() -> postService.getPost(USER_ID, POST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorMessage.POST_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class editPostTest {

        @Test
        @DisplayName("게시글 수정 성공")
		void editPost_성공() {
			PostCreateEditRequestDto requestDto = PostFixture.requestDto(VALID_PROBLEM_NUMBER, VALID_CATEGORIES, PostFixture.UPDATED_TITLE, PostFixture.UPDATED_CONTENT);

			given(postRepository.findByIdWithUserAndCategories(POST_ID)).willReturn(Optional.of(post));
			given(problemRepository.findByNumber(VALID_PROBLEM_NUMBER)).willReturn(Optional.of(problem));
			given(categoryRepository.findByNameIn(VALID_CATEGORIES)).willReturn(List.of(category));

			postService.editPost(USER_ID, POST_ID, requestDto);

			assertThat(post.getProblemNumber()).isEqualTo(VALID_PROBLEM_NUMBER);
			assertThat(post.getTitle()).isEqualTo(PostFixture.UPDATED_TITLE);
			assertThat(post.getContent()).isEqualTo(PostFixture.UPDATED_CONTENT);
			assertThat(post.getPostCategories().get(0).getCategory().getName()).isEqualTo(CATEGORY_DP);
			assertThat(post.getUpdatedAt()).isNotNull();
		}

        @Test
        @DisplayName("게시글이 존재하지 않으면 예외 발생")
        void editPost_게시글없음() {
            // given
            given(postRepository.findByIdWithUserAndCategories(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.editPost(USER_ID, POST_ID, PostCreateEditRequestDto.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorMessage.POST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 수정 시도 시 예외 발생")
        void editPost_권한없음() {
            // given
            User anotherUser = UserFixture.anotherUser();
            post.setUser(anotherUser);

            given(postRepository.findByIdWithUserAndCategories(POST_ID)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.editPost(USER_ID, POST_ID, PostCreateEditRequestDto.builder().build()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage(ErrorMessage.NO_POST_PERMISSION.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 문제 번호 수정 시 예외 발생")
        void editPost_존재하지않는문제번호() {
            // given
			requestDto = PostFixture.requestDto(INVALID_PROBLEM_NUMBER, VALID_CATEGORIES, PostFixture.TEST_TITLE, PostFixture.TEST_CONTENT);

            given(postRepository.findByIdWithUserAndCategories(POST_ID)).willReturn(Optional.of(post));
            given(problemRepository.findByNumber(INVALID_PROBLEM_NUMBER)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.editPost(USER_ID, POST_ID, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessage.INVALID_PROBLEM_INCLUDED.getMessage());
        }

		@Test
		@DisplayName("존재하지 않는 카테고리 수정 시 예외 발생")
		void editPost_존재하지않는카테고리() {
			PostCreateEditRequestDto requestDto = PostFixture.requestDto(30000L, INVALID_CATEGORIES, PostFixture.UPDATED_TITLE, PostFixture.UPDATED_CONTENT);

			given(postRepository.findByIdWithUserAndCategories(POST_ID)).willReturn(Optional.of(post));
			given(problemRepository.findByNumber(30000L)).willReturn(Optional.of(problem));
			given(categoryRepository.findByNameIn(INVALID_CATEGORIES)).willReturn(List.of(category)); // 1개만 반환

			assertThatThrownBy(() -> postService.editPost(USER_ID, POST_ID, requestDto))
				.isInstanceOf(BadRequestException.class)
				.hasMessage(ErrorMessage.INVALID_CATEGORY_INCLUDED.getMessage());
		}


    }

    @Nested
    @DisplayName("게시글 삭제")
    class deletePostTest {

        @Test
        void deletePost_성공(){
            // given
            given(postRepository.findByIdWithUser(POST_ID)).willReturn(Optional.of(post));

            // when
            postService.deletePost(USER_ID, POST_ID);

            // then
            assertThat(post.getDeletedAt()).isNotNull();
            verify(postRepository).findByIdWithUser(POST_ID);
        }

        @Test
        void deletePost_삭제권한없음(){
            User anotherUser = UserFixture.anotherUser();

            post.setUser(anotherUser);

            given(postRepository.findByIdWithUser(POST_ID)).willReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.deletePost(USER_ID, POST_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage(ErrorMessage.NO_POST_PERMISSION.getMessage());
        }

        @Test
        void deletePost_존재하지않는게시글(){
            given(postRepository.findByIdWithUser(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.deletePost(USER_ID, POST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorMessage.POST_NOT_FOUND.getMessage());
        }
    }
}
