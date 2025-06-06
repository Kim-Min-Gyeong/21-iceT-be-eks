package icet.koco.posts.repository;

import icet.koco.posts.entity.Post;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p " +
        "JOIN FETCH p.user " +                         // 작성자
        "LEFT JOIN FETCH p.postCategories pc " +       // 중간 엔티티
        "LEFT JOIN FETCH pc.category " +               // 연결된 카테고리
        "WHERE p.id = :postId AND p.deletedAt IS NULL")
    Optional<Post> findByIdWithUserAndCategories(@Param("postId") Long postId);
}
