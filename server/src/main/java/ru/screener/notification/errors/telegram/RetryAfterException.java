package ru.screener.notification.errors.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class RetryAfterException extends RuntimeException {

    private static final int MIN_SECONDS = 1;
    private static final int DEFAULT_RETRY_SECONDS = 3;

    private final int seconds;

    private RetryAfterException(int seconds) {
        this.seconds = Math.max(MIN_SECONDS, seconds);
    }

    public int seconds() {
        return seconds;
    }

    public static RetryAfterException from(ClientResponse resp, String body) {
        Integer jsonSeconds = JsonRetryAfterExtractor.tryExtract(body);
        if (jsonSeconds != null) {
            return new RetryAfterException(jsonSeconds);
        }

        String header = resp.headers().asHttpHeaders().getFirst("Retry-After");
        if (header != null) {
            try {
                return new RetryAfterException(Integer.parseInt(header.trim()));
            }
            catch (NumberFormatException ignore) {
                try {
                    long epoch = DateTimeFormatter.RFC_1123_DATE_TIME
                            .parse(header, Instant::from).getEpochSecond();
                    int seconds = (int) Math.max(MIN_SECONDS, epoch - Instant.now().getEpochSecond());
                    return new RetryAfterException(seconds);
                } catch (Exception ignored) {}
            }
        }

        return new RetryAfterException(DEFAULT_RETRY_SECONDS);
    }

    public static final class JsonRetryAfterExtractor {
        static Integer tryExtract(String body) {
            try {
                var node = new ObjectMapper().readTree(body);
                var path = node.path("parameters").path("retry_after");
                return path.isInt() ? path.asInt() : null;
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
