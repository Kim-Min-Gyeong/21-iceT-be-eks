package icet.koco.util.test;


import icet.koco.user.entity.User;
import icet.koco.user.repository.UserRepository;
import icet.koco.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test-auth")
@Tag(name = "Test용")
public class TestAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/token")
    @Operation(summary = "테스트용 access_token을 발급하게 하는 API입니다. ")
    public ResponseEntity<?> createTestToken(@RequestParam Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        String token = jwtTokenProvider.createAccessToken(user);
        System.out.println("testAuth token: " + token);
        return ResponseEntity.ok(Map.of("access_token", token));
    }
}
