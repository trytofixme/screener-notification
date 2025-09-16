package ru.screener.notification.utils.request;

import lombok.experimental.UtilityClass;
import ru.screener.notification.dto.telegram.TelegramSendRequestDto;

@UtilityClass
public class TelegramRequestBuilder {

    public TelegramSendRequestDto buildPlain(Long chatId, String text) {
        return base(chatId, text);
    }

    private TelegramSendRequestDto base(Long chatId, String text) {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId is null");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is blank");
        }

        return TelegramSendRequestDto.builder()
                .chat_id(chatId)
                .text(text)
                .disable_web_page_preview(true)
                .build();
    }
}