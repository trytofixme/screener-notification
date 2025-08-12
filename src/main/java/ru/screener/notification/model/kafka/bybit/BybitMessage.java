package ru.screener.notification.model.kafka.bybit;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class BybitMessage {

    private String symbol;
    private String strategyName;
    private BigDecimal value;
}
