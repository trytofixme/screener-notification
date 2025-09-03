package ru.screener.notification.messaging.bybit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.model.bybit.BybitEvent;
import ru.screener.notification.model.strategies.Direction;

import java.text.DecimalFormat;

@Component
@Slf4j
public class BybitMessageBuilder implements MessageBuilder<BybitEvent> {

    private static final DecimalFormat DF = new DecimalFormat("0.##");

    @Override
    public String buildMessage(BybitEvent event) {
        return switch (event.getStrategy()) {
            case RSI -> formatRsi(event);
            case PUMP_DUMP -> null;
            case OI -> null;
        };
    }

    private String formatRsi(BybitEvent event) {
        String circle = circle(event.getDirection());
        String arrow = arrow(event.getDirection());
        String rsi = DF.format(event.getRsi());
        return String.format("%s #%s - %s %s%n%s RSI %s%n📢 За сутки: %d", circle, event.getSymbol(), event.getTimeframe(), circle,
                arrow, rsi, event.getSignalNumber());
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
