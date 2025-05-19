package icet.koco.util.uploader;

import icet.koco.global.dto.ApiResponse;
import icet.koco.util.uploader.dto.S3UrlResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/backend/v1/upload")
public class S3UploadController {

    private final S3PresignedUrlService s3Service;

    @GetMapping("/presigned-url")
    public ResponseEntity<?> getPresignedUrl(@RequestParam String originalFileName) {
        String uuidFileName = UUID.randomUUID() + "_" + originalFileName;

        String presignedUrl = s3Service.generatePresignedUrl(uuidFileName);
        String fileUrl = s3Service.getFileUrl(uuidFileName);

        S3UrlResponseDto responseDto = S3UrlResponseDto.builder()
                .presignedUrl(presignedUrl)
                .fileUrl(fileUrl)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("S3_PRESIGNED_URL_SUCCESS", "AWS_S3 presigned url 조회 성공", responseDto)
        );
    }

}

