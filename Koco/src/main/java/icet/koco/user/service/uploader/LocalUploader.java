package icet.koco.user.service.uploader;

import java.io.IOException;
import java.nio.file.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@org.springframework.context.annotation.Profile("local")
public class LocalUploader implements ImageUploader {

    private static final String LOCAL_DIR = "/Users/yeon/Desktop/Koco";
    private static final String BASE_URL = "http://localhost:8080/images/";

    @Override
    public String upload(MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(LOCAL_DIR, fileName);

            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            return BASE_URL + fileName;
        } catch (IOException e) {
            throw new RuntimeException("로컬 이미지 저장 실패", e);
        }
    }
}