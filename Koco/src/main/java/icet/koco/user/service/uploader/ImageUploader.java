package icet.koco.user.service.uploader;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploader {
    String upload(MultipartFile file);
}