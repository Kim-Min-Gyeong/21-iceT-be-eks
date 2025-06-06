package icet.koco.posts.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostCreateRequestDto {
    private Long problemNumber;
    private String title;
    private String content;
    private List<String> category;
}
