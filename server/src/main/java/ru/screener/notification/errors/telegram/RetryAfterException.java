package ru.screener.notification.errors.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class RetryAfterException extends RuntimeException {
    private final int seconds;

    public RetryAfterException(ClientResponse clientResponse) {
        this.seconds = Optional.ofNullable(clientResponse.headers().asHttpHeaders().getFirst("Retry-After"))
                .flatMap(v -> {
                    try { return Optional.of(Integer.parseInt(v.trim())); }
                    catch (NumberFormatException e) { return Optional.empty(); }
                })
                .orElse(3);
    }

    public RetryAfterException(int seconds) {
        this.seconds = seconds;
    }

    public int seconds() {
        return seconds;
    }
}
