package ru.screener.notification.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record TelegramSendRequestDto(
        Long chat_id,
        String text,
        String parse_mode,
        Boolean disable_web_page_preview,
        Boolean disable_notification,
        Integer message_thread_id,
        Object reply_markup
) {}
