package icet.koco.posts.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import icet.koco.posts.entity.Post;
import icet.koco.posts.entity.QLike;
import icet.koco.posts.entity.QPost;
import icet.koco.posts.entity.QPostCategory;
import icet.koco.problemSet.entity.QCategory;
import icet.koco.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomPostRepositoryImpl implements CustomPostRepository {
    private final JPAQueryFactory queryFactory;
    @Override
    public List<Post> searchPosts(List<String> categoryNames, String keyword, Long cursorId, int size) {
        QPost post = QPost.post;
        QCategory category = QCategory.category;
        QPostCategory postCategory = QPostCategory.postCategory;

        var subQuery = queryFactory
                .select(post.id)
                .from(post)
                .join(post.postCategories, postCategory)
                .join(postCategory.category, category)
                .where(
                        post.deletedAt.isNull(),
                        cursorId != null ? post.id.loe(cursorId) : null,
                        StringUtils.hasText(keyword)
                                ? (keyword.matches("\\d+")
                                ? post.problemNumber.eq(Long.parseLong(keyword))
                                .or(post.title.containsIgnoreCase(keyword))
                                : post.title.containsIgnoreCase(keyword))
                                : null,
                        (categoryNames != null && !categoryNames.isEmpty())
                                ? category.name.in(categoryNames)
                                : null
                )
                .groupBy(post.id);

        if (categoryNames != null && !categoryNames.isEmpty()) {
            subQuery.having(postCategory.category.id.countDistinct().eq((long) categoryNames.size()));
        }

        List<Long> postIds = subQuery
                .orderBy(post.id.desc())
                .limit(size)
                .fetch();

        if (postIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(post)
                .distinct()
                .leftJoin(post.postCategories, postCategory).fetchJoin()
                .leftJoin(postCategory.category, category).fetchJoin()
                .where(post.id.in(postIds))
                .orderBy(post.id.desc())
                .fetch();
    }

    @Override
    public List<Post> getMyPosts(Long userId, Long cursorId, int size) {
        QPost post = QPost.post;
        QCategory category = QCategory.category;
        QPostCategory postCategory = QPostCategory.postCategory;

        var subQuery = queryFactory
                .select(post.id)
                .from(post)
                .join(post.postCategories, postCategory)
                .join(postCategory.category, category)
                .where(
                        post.user.id.eq(userId),
                        post.deletedAt.isNull(),
                        cursorId != null ? post.id.loe(cursorId) : null
                )
                .groupBy(post.id);

        List<Long> postIds = subQuery
                .orderBy(post.id.desc())
                .limit(size)
                .fetch();

        if (postIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(post)
                .leftJoin(post.postCategories, postCategory).fetchJoin()
                .leftJoin(postCategory.category, category).fetchJoin()
                .where(post.id.in(postIds))
                .orderBy(post.id.desc())
                .fetch();
    }

    @Override
    public List<Post> findTopPostsByLikesLastWeek(LocalDateTime start, LocalDateTime end, int limit) {
        QPost post = QPost.post;
        QLike like = QLike.like;

        List<Long> postIds = queryFactory
                .select(like.post.id)
                .from(like)
                .where(like.createdAt.between(start, end), like.post.deletedAt.isNull())
                .groupBy(like.post.id)
                .orderBy(like.post.id.count().desc())
                .limit(limit)
                .fetch();

        if (postIds.isEmpty()) return List.of();

        return queryFactory
                .selectFrom(post)
                .leftJoin(post.postCategories, QPostCategory.postCategory).fetchJoin()
                .leftJoin(QPostCategory.postCategory.category, QCategory.category).fetchJoin()
                .join(post.user, QUser.user).fetchJoin()
                .where(post.id.in(postIds))
                .fetch();
    }

}



/*        return queryFactory
                .selectFrom(post)
                .leftJoin(post.user, user).fetchJoin()
                .leftJoin(post.postCategories, postCategory).fetchJoin()
                .leftJoin(postCategory.category, category).fetchJoin()
                .where(
                        post.user.id.eq(userId),
                        post.deletedAt.isNull(),
                        cursorId != null ? post.id.loe(cursorId) : null
                )
                .orderBy(post.id.desc())
                .limit(size)
                .fetch();

 */