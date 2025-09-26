package ru.screener.notification.messaging.rsi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.dto.rsi.RsiNotificationDto;
import ru.screener.notification.model.strategies.Direction;

import java.text.DecimalFormat;

@Component
@Slf4j
public class RsiMessageBuilder implements MessageBuilder<RsiNotificationDto> {

    private static final DecimalFormat DF = new DecimalFormat("0.##");

    @Override
    public String buildMessage(RsiNotificationDto event) {
        return switch (event.getStrategy()) {
            case RSI -> formatRsi(event);
        };
    }

    private String formatRsi(RsiNotificationDto event) {
        String circle = circle(event.getDirection());
        String arrow = arrow(event.getDirection());
        String rsi = DF.format(event.getRsi());
        return String.format("%s #%s - %s %s%n%s RSI %s%n📢 За сутки: %d", circle, event.getSymbol(), event.getTimeframe(), circle,
                arrow, rsi, event.getSignalNumber());
    }

    private String circle(Direction direction) {
        return direction == Direction.UP ? "🟢" : "🔴";
    }

    private String arrow(Direction direction) {
        return direction == Direction.UP ? "🔺" : "🔻";
    }
}
