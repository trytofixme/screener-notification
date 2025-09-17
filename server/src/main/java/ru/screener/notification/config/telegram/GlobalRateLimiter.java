package ru.screener.notification.config.telegram;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CancellationException;

@Slf4j
@Component
public class GlobalRateLimiter {

    private final int permitsPerSecond;
    private final Sinks.Many<Void> permitSink;
    private volatile boolean active = true;

    public GlobalRateLimiter(TelegramProperties props) {
        this.permitsPerSecond = Math.max(1, props.getRatelimit().getMaxGlobalRps());
        this.permitSink = Sinks.many().multicast().onBackpressureBuffer(permitsPerSecond);
    }

    @PostConstruct
    public void startRefillTask() {
        Flux.interval(Duration.ZERO, Duration.ofSeconds(1), Schedulers.single())
                .takeWhile(tick -> active)
                .subscribe(
                        tick -> { // onNext
                            for (int i = 0; i < permitsPerSecond; i++) {
                                // tryEmitNext - неблокирующий и безопасный
                                permitSink.tryEmitNext(null);
                            }
                        },
                        error -> { // onError
                            log.error("CRITICAL: Error in GlobalRateLimiter refill task. The limiter will stop working!", error);
                            active = false;
                            permitSink.tryEmitError(new CancellationException("Rate limiter refill task failed"));
                        }
                );
    }

    public Mono<Void> acquire() {
        return permitSink.asFlux().next();
    }

    @PreDestroy
    public void destroy() {
        active = false;
        permitSink.tryEmitError(new CancellationException("GlobalRateLimiter destroyed"));
    }
}
