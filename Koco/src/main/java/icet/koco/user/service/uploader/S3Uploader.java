package icet.koco.user.service.uploader;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Component
//@Profile("prod")
@RequiredArgsConstructor
public class S3Uploader implements ImageUploader {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile file) {
        try {
            String fileName = "profile/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            System.out.println(">>> 파일 이름: " + fileName);
            System.out.println(">>> 버킷 이름: " + bucket);
            System.out.println(">>> 파일 크기: " + file.getSize());
            System.out.println(">>> 파일 타입: " + file.getContentType());

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

            String url = amazonS3.getUrl(bucket, fileName).toString();
            System.out.println(">>> S3 업로드 완료: " + url);
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

}
