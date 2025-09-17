package ru.screener.notification.config.telegram;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CancellationException;

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
        Flux.interval(Duration.ofSeconds(1), Schedulers.single())
                .takeWhile(tick -> active)
                .subscribe(tick -> {
                    for (int i = 0; i < permitsPerSecond; i++) {
                        permitSink.tryEmitNext(null);
                    }
                });
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
