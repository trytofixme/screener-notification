package ru.screener.notification.model.kafka.bybit;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class BybitEvent {

    private UUID eventId;
    private String telegramId;
    private EventInfo eventInfo;

    @Data
    @Accessors(chain = true)
    public static class EventInfo {
        private String symbol;
        private String strategyName;
        private BigDecimal value;
    }
}
