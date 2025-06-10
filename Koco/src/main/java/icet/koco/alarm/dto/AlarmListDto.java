package icet.koco.alarm.dto;

import icet.koco.enums.AlarmType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmListDto {
    private Long cursorId;
    private int size;
    private List<AlarmDto> alarms;

    static private class AlarmDto {
        private Long id;
        private Long postId;
        private String postTitle;
        private Long receiverId;
        private Long senderId;
        private String senderNickname;
        private AlarmType alarmType;
        private LocalDateTime createdAt;
    }
}
