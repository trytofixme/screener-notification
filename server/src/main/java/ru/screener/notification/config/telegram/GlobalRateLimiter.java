package ru.screener.notification.config.telegram;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
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
        int cfg = Math.max(1, props.getRatelimit().getMaxGlobalRps());
        this.permitsPerSecond = cfg;
        this.bucket = new Semaphore(cfg, true);

        this.refillTask = Flux.interval(Duration.ZERO, Duration.ofSeconds(1), scheduler)
                .subscribe(t -> {
                    int deficit = permitsPerSecond - bucket.availablePermits();
                    if (deficit > 0) bucket.release(deficit);

                    while (!waiters.isEmpty() && bucket.tryAcquire()) {
                        Sinks.One<Void> sink = waiters.poll();
                        if (sink == null) {
                            bucket.release();
                            break;
                        }
                        if (sink.tryEmitEmpty().isFailure()) {
                            bucket.release();
                        }
                    }

                });
    }

    public Mono<Void> acquire() {
        if (bucket.tryAcquire()) return Mono.empty();

        Sinks.One<Void> sink = Sinks.one();
        return sink.asMono()
                .doOnSubscribe(s -> waiters.add(sink))
                .doFinally(sig -> {
                    if (sig == SignalType.CANCEL) {
                        waiters.remove(sink);
                    }
                });
    }

    @Override
    public void destroy() {
        if (refillTask != null) refillTask.dispose();
        scheduler.dispose();
        for (Sinks.One<Void> s : waiters) {
            s.tryEmitError(new CancellationException("GlobalRateLimiter destroyed"));
        }
        waiters.clear();
    }
}
