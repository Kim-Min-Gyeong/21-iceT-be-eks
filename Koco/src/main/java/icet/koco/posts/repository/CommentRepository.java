package icet.koco.posts.repository;

import icet.koco.posts.entity.Comment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Integer countByPostId(Long postId);

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);
}
