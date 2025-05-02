package icet.koco.user.service;

import icet.koco.auth.entity.OAuth;
import icet.koco.auth.repository.OAuthRepository;
import icet.koco.auth.service.KakaoOAuthClient;
import icet.koco.global.exception.ResourceNotFoundException;
import icet.koco.global.exception.UnauthorizedException;
import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final OAuthRepository oAuthRepository;

    @Transactional
    public void deleteUser(Long userId) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("해당 유저 정보를 찾을 수 없습니다."));

        // 2. OAuth 정보 조회 (OAuth 테이블에서 userId로 조회)
        OAuth oauth = oAuthRepository.findByUserId(userId)
            .orElseThrow(() -> new UnauthorizedException("OAuth 정보가 존재하지 않습니다."));


        // 3. Kakao unlink 호출 (예외 무시)
        try {
            kakaoOAuthClient.unlinkUser(oauth.getProviderId()); // Long 또는 String
        } catch (Exception e) {
            log.warn(">>>>> Kakao unlink 실패: {}", e.getMessage());
        }
        // Redis refreshToken 삭제
        redisTemplate.delete(userId.toString());

        // soft delete 처리 (삭제요구한 일시)
        user.setDeletedAt(LocalDateTime.now());

        // user에 정보 저장
        userRepository.save(user);
    }

}

