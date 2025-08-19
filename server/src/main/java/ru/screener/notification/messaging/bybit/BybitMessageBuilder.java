package ru.screener.notification.messaging.bybit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.model.kafka.bybit.BybitMessage;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class BybitMessageBuilder implements MessageBuilder<BybitMessage> {

    private static final String MESSAGE_LABEL = "bybit.messages";

    private final MessageSource messageSource;

    @Override
    public Class<BybitMessage> getInstance() {
        return BybitMessage.class;
    }

    public String buildMessage(BybitMessage event) {

        Object[] args = new Object[] {
                event.getSymbol(),
                event.getStrategyName(),
                event.getValue()
        };

        String message = messageSource.getMessage(MESSAGE_LABEL, args, Locale.getDefault());

        log.info("Message {} was written", message);

        return message;
    }
}
