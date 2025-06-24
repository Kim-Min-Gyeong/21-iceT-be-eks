package icet.koco.util.s3uploader.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class S3UrlResponseDto {
    private String fileUrl;
    private String presignedUrl;
}
