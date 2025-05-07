package icet.koco.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCategoryStatDto implements UserCategoryStatProjection {
    private Long categoryId;
    private String categoryName;
    private Double correctRate;

    @Override
    public Long getCategoryId() {
        return categoryId;
    }

    @Override
    public String getCategoryName() {
        return categoryName;
    }

    @Override
    public Double getCorrectRate() {
        return correctRate;
    }
}
