package icet.koco.posts.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import icet.koco.posts.dto.post.TopPostResponseDto;
import icet.koco.posts.entity.Post;
import icet.koco.posts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyTopPostCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "top_posts:week:";

    public List<TopPostResponseDto> getOrGenerateTopPosts() {
        String key = getCurrentWeekKey();

        List<TopPostResponseDto> cached = getFromRedis(key);

        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<TopPostResponseDto> topPosts = calculateTopPostsOfLastWeek();

        try {
            String json = objectMapper.writeValueAsString(topPosts);
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));
        } catch (Exception e) {
            log.error(">>>>> Redis 저장 실패: {}", e.getMessage());
        }

        return topPosts;
    }

    private List<TopPostResponseDto> getFromRedis(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, new TypeReference<>() {
                });
            }
        } catch (Exception e) {
            log.error(">>>>> Redis 파싱 실패: {}",e.getMessage());
        }
        return null;
    }


    private List<TopPostResponseDto> calculateTopPostsOfLastWeek() {
        System.out.println("WeeklyTopPostCacheService.calculateTopPostsOfLastWeek");
        LocalDateTime start = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime end = LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY).atTime(23, 59, 59);

        List<Post> posts = postRepository.findTopPostsByLikesLastWeek(start, end, 5);
        System.out.println("post.size() = " + posts.size());

        return posts.stream().map(TopPostResponseDto::from).toList();
    }

    private String getCurrentWeekKey() {
        System.out.println("WeeklyTopPostCacheService.getCurrentWeekKey");
        LocalDate monday = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        return REDIS_KEY_PREFIX + monday;
    }

}