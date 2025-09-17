package ru.screener.notification.config.telegram;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

@Component
public class GlobalRateLimiter implements DisposableBean {
    private final int permitsPerSecond;
    private final Semaphore bucket;
    private final Scheduler scheduler = Schedulers.newSingle("tg-global-rl");
    private final Disposable refillTask;
    private final ConcurrentLinkedQueue<Sinks.One<Void>> waiters = new ConcurrentLinkedQueue<>();

    public GlobalRateLimiter(TelegramProperties props) {
        this.permitsPerSecond = Math.max(1, props.getMaxGlobalRps());
        this.bucket = new Semaphore(permitsPerSecond, true);
        this.refillTask = Flux.interval(Duration.ZERO, Duration.ofSeconds(1), scheduler)
                .subscribe(t -> {
                    int deficit = permitsPerSecond - bucket.availablePermits();
                    if (deficit > 0) bucket.release(deficit);
                    while (bucket.tryAcquire()) {
                        Sinks.One<Void> s = waiters.poll();
                        if (s == null) { bucket.release(); break; }
                        if (s.tryEmitEmpty().isFailure()) bucket.release();
                    }
                });
    }

    public Mono<Void> acquire() {
        return Mono.create(sink -> {
            if (bucket.tryAcquire()) { sink.success(); return; }

            Sinks.One<Void> waiter = Sinks.one();
            waiters.offer(waiter);

            // если подписчик отвалился — убираем waiter (best-effort)
            sink.onCancel(() -> waiter.tryEmitError(new CancellationException()));
            sink.onDispose(() -> waiter.tryEmitError(new CancellationException()));

            waiter.asMono().subscribe(
                    v -> sink.success(),
                    e -> { /* игнор */ }
            );
        });
    }


    @Override public void destroy() { refillTask.dispose(); scheduler.dispose(); }
}

