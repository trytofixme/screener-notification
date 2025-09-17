package ru.screener.notification.config.telegram;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PerChatLimiter {

    private final long minIntervalMs;
    private final ConcurrentHashMap<Long, AtomicLong> lastSentAt = new ConcurrentHashMap<>();

    public PerChatLimiter(TelegramProperties props) {
        this.minIntervalMs = props.getRatelimit().getPerChatMinIntervalMs();
    }

    public Mono<Void> acquire(long chatId) {
        return Mono.defer(() -> {
            long now = System.currentTimeMillis();
            AtomicLong lastRef = lastSentAt.computeIfAbsent(chatId, id -> new AtomicLong(0));
            long last = lastRef.get();
            long wait = last + minIntervalMs - now;
            if (wait <= 0 && lastRef.compareAndSet(last, now)) {
                return Mono.empty();
            }
            long delay = Math.max(1, wait);
            return Mono.delay(Duration.ofMillis(delay))
                    .then(Mono.fromRunnable(() -> lastRef.set(System.currentTimeMillis())))
                    .then();
        });
    }
}

