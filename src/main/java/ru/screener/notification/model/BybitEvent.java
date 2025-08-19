package ru.screener.notification.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.screener.notification.model.enums.Direction;
import ru.screener.notification.model.enums.Strategy;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BybitEvent {
    private Strategy strategy;
    private String symbol;
    private String timeframe;
    private Direction direction;
    private BigDecimal percent; // percent change for PUMP_DUMP and OI
    private BigDecimal priceFrom;
    private BigDecimal priceTo;
    private BigDecimal rsi; // RSI value
    private BigDecimal openInterestUsd; // OI value in millions of dollars
    private int signalNumber;
}
