package ru.screener.notification.dto.rsi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.screener.notification.model.strategies.Direction;
import ru.screener.notification.model.strategies.Strategy;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RsiNotificationDto {

    private Long telegramId;
    private Strategy strategy;
    private String symbol;
    private String timeframe;
    private Direction direction;
    private BigDecimal rsi;
    private int signalNumber;
}
