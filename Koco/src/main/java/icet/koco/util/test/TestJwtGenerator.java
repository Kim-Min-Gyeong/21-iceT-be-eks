package icet.koco.util.test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Component
@Profile({"dev", "test"})
public class TestJwtGenerator implements CommandLineRunner {
    private final String secret = "asgfdvhbjghbnmdfbgvehajkldkjnbvsabnmmnbas";

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    long accessTokenValidity = 1000 * 60 * 30;            // 30분
    long refreshTokenValidity = 1000 * 60 * 60 * 24 * 7;  // 7일

    @Override
    public void run(String... args) {
        generateTokenCsv();
    }

    public Map<String, String> createTokenSet(Long userId) {
        Date now = new Date();

        String accessToken = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenValidity))
                .signWith(key)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenValidity))
                .signWith(key)
                .compact();

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    public void generateTokenCsv() {
        System.out.println("userId,accessToken,refreshToken");

        for (long userId = 200; userId <= 300; userId++) {
            Map<String, String> tokenSet = createTokenSet(userId);
            System.out.println(userId + "," + tokenSet.get("accessToken") + "," + tokenSet.get("refreshToken"));
        }
    }
}
