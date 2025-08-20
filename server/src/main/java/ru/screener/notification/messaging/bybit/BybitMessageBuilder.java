package ru.screener.notification.messaging.bybit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.model.strategies.Direction;
import ru.screener.notification.model.bybit.BybitEvent;

import java.text.DecimalFormat;
import java.util.Locale;

@Component
@Slf4j
public class BybitMessageBuilder implements MessageBuilder<BybitEvent> {

    private static final DecimalFormat DF = new DecimalFormat("0.##");

    @Override
    public String buildMessage(BybitEvent event) {
        return switch (event.getStrategy()) {
            case PUMP_DUMP -> formatPumpDump(event);
            case RSI -> formatRsi(event);
            case OI -> formatOi(event);
        };
    }

    private String formatPumpDump(BybitEvent event) {
        String circle = circle(event.getDirection());
        String arrow = arrow(event.getDirection());
        String action = event.getDirection() == Direction.UP ? "ПАМП" : "ДАМП";
        String percent = sign(event.getDirection()) + DF.format(event.getPercent()) + "%";
        String price = String.format(Locale.US, "(%s$-%s$)", DF.format(event.getPriceFrom()), DF.format(event.getPriceTo()));
        return String.format("%s #%s - %s %s%n%s %s %s %n💰 %s %n📢 За сутки: %d", circle, event.getSymbol(), event.getTimeframe(), circle,
                arrow, action, percent, price, event.getSignalNumber());
    }

    private String formatRsi(BybitEvent event) {
        String circle = circle(event.getDirection());
        String arrow = arrow(event.getDirection());
        String rsi = DF.format(event.getRsi());
        return String.format("%s #%s - %s %s%n%s RSI %s%n📢 За сутки: %d", circle, event.getSymbol(), event.getTimeframe(), circle,
                arrow, rsi, event.getSignalNumber());
    }

    private String formatOi(BybitEvent event) {
        String arrow = arrow(event.getDirection());
        String percent = sign(event.getDirection()) + DF.format(event.getPercent()) + "%";
        String oi = DF.format(event.getOpenInterestUsd()) + " млн$";
        String price = String.format(Locale.US, "(%s$-%s$)", DF.format(event.getPriceFrom()), DF.format(event.getPriceTo()));
        return String.format("🟡 #%s - %s 🟡%n%s ОИ %s (%s)%n💰 %s %n📢 За сутки: %d", event.getSymbol(), event.getTimeframe(), arrow,
                percent, oi, price, event.getSignalNumber());
    }

    private String circle(Direction direction) {
        return direction == Direction.UP ? "🔴" : "🟢";
    }

    private String arrow(Direction direction) {
        return direction == Direction.UP ? "🔺" : "🔻";
    }

    private String sign(Direction direction) {
        return direction == Direction.UP ? "+" : "-";
    }
}
