package icet.koco.user.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponseDto {
    private Long userId;
    private String nickname;
    private String statusMessage;
    private String profileImgUrl;
    private Long todayProblemSetId;
    private List<CategoryStat> studyStats;

    @Getter
    @Builder
    public static class CategoryStat {
        private Long categoryId;
        private String categoryName;
        private Double correctRate;
    }
}
