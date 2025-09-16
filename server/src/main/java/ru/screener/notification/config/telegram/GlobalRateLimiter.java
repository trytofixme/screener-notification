package ru.screener.notification.config.telegram;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.screener.notification.errors.telegram.TooManyRequestsException;

import java.time.Duration;
import java.util.concurrent.Semaphore;

@Component
public class GlobalRateLimiter {

    private final Semaphore bucket;

    public GlobalRateLimiter(TelegramProperties telegramProperties) {
        this.bucket = new Semaphore(telegramProperties.getRatelimit().getMaxGlobalRps());
        Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                .subscribe(t -> refill(telegramProperties.getRatelimit().getMaxGlobalRps()));
    }

    private void refill(int permitsPerSecond) {
        int deficit = permitsPerSecond - bucket.availablePermits();
        if (deficit > 0) {
            bucket.release(deficit);
        }
    }

    public Mono<Void> acquire() {
        return Mono.create(sink -> {
            if (bucket.tryAcquire()) sink.success();
            else {
                Flux.interval(Duration.ofMillis(50)).take(20)
                        .filter(i -> bucket.tryAcquire())
                        .next().subscribe(v -> sink.success(), sink::error,
                                () -> sink.error(new TooManyRequestsException()));
            }
        });
    }
}

