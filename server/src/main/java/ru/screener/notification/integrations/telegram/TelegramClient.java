package ru.screener.notification.integrations.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import ru.screener.notification.config.telegram.TelegramProperties;
import ru.screener.notification.dto.telegram.TelegramSendRequestDto;
import ru.screener.notification.errors.telegram.RetryAfterException;
import ru.screener.notification.errors.telegram.TransientTelegramException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClient {

    private final static int TOO_MANY_REQUEST_EXCEPTION_CODE = 429;

    private final WebClient webClient;
    private final TelegramProperties telegramProperties;

    public Mono<Void> sendMessage(TelegramSendRequestDto dto) {
        return webClient.post()
                .uri(telegramProperties.getMethods().getSendMessage())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .onStatus(statusCode -> statusCode.value() == TOO_MANY_REQUEST_EXCEPTION_CODE, response ->
                        response.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(RetryAfterException.from(response, body))))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new TransientTelegramException("5xx from Telegram")))
                .bodyToMono(Void.class)
                .onErrorResume(WebClientRequestException.class,
                        e -> Mono.error(new TransientTelegramException("I/O exception", e)));
    }
}
