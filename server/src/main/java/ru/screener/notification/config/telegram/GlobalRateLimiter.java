package ru.screener.notification.config.telegram;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Semaphore;

@Component
public class GlobalRateLimiter implements DisposableBean {

    private final Semaphore bucket;
    private final int permitsPerSecond;
    private final Scheduler scheduler = Schedulers.newSingle("tg-global-rl");
    private final Disposable refillTask;

    public GlobalRateLimiter(TelegramProperties props) {
        this.permitsPerSecond = props.getRatelimit().getMaxGlobalRps();
        this.bucket = new Semaphore(permitsPerSecond);
        this.refillTask = Flux.interval(Duration.ZERO, Duration.ofSeconds(1), scheduler)
                .subscribe(t -> {
                    int deficit = permitsPerSecond - bucket.availablePermits();
                    if (deficit > 0) bucket.release(deficit);
                });
    }

    public Mono<Void> acquire() {
        return Mono.defer(() -> {
            if (bucket.tryAcquire()) return Mono.empty();
            long stepMs = Math.max(25, 1000L / Math.max(1, permitsPerSecond));
            return Mono.delay(Duration.ofMillis(stepMs), scheduler).then(acquire());
        });
    }

    @Override
    public void destroy() {
        if (refillTask != null) refillTask.dispose();
        scheduler.dispose();
    }
}
