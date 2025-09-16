package ru.screener.notification.utils.request;

import lombok.experimental.UtilityClass;
import ru.screener.notification.model.dto.TelegramSendRequestDto;

@UtilityClass
public class TelegramRequestBuilder {
    public static final int TELEGRAM_TEXT_LIMIT = 4096;

    public TelegramSendRequestDto buildPlain(Long chatId, String text) {
        return base(chatId, text, null, true, null, null);
    }

    public TelegramSendRequestDto buildMarkdown(Long chatId, String text, boolean disablePreview) {
        return base(chatId, text, "MarkdownV2", disablePreview, null, null);
    }

    public TelegramSendRequestDto base(
            Long chatId,
            String text,
            String parseMode,
            Boolean disablePreview,
            Boolean disableNotification,
            Object replyMarkup
    ) {
        if (chatId == null) throw new IllegalArgumentException("chatId is null");
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text is blank");
        String safe = text.length() > TELEGRAM_TEXT_LIMIT
                ? text.substring(0, TELEGRAM_TEXT_LIMIT - 3) + "..."
                : text;

        return TelegramSendRequestDto.builder()
                .chat_id(chatId)
                .text(safe)
                .parse_mode(parseMode)
                .disable_web_page_preview(disablePreview)
                .disable_notification(disableNotification)
                .reply_markup(replyMarkup)
                .build();
    }
}