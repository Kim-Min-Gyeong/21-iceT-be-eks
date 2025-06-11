package icet.koco.alarm.repository;

import icet.koco.alarm.entity.Alarm;
import java.util.List;

public interface AlarmRepositoryCustom {
    List<Alarm> findByReceiverIdWithCursor(Long receiverId, Long cursorId, int size);
}
