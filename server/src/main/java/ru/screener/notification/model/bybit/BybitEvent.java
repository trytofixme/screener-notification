package ru.screener.notification.model.bybit;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.screener.notification.model.strategies.Direction;
import ru.screener.notification.model.strategies.Strategy;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class BybitEvent {

    private String telegramId;
    private Strategy strategy;
    private String symbol;
    private String timeframe;
    private Direction direction;
    private BigDecimal percent; // percent change for PUMP_DUMP and OI
    private BigDecimal priceFrom;
    private BigDecimal priceTo;
    private BigDecimal rsi;
    private BigDecimal openInterestUsd;
    private int signalNumber;
}
