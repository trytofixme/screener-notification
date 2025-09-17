package ru.screener.notification.config.telegram;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PerChatLimiter {

    private final long minIntervalMs;
    private final ConcurrentHashMap<Long, AtomicLong> nextAllowedAt = new ConcurrentHashMap<>();

    public PerChatLimiter(TelegramProperties props) {
        this.minIntervalMs = Math.max(1, props.getRatelimit().getPerChatMinIntervalMs());
    }

    public Mono<Void> acquire(long chatId) {
        return Mono.defer(() -> {
            long now = System.currentTimeMillis();
            AtomicLong ref = nextAllowedAt.computeIfAbsent(chatId, id -> new AtomicLong(0));
            
            long mySlot = ref.updateAndGet(prev -> {
                long base = Math.max(prev, now);
                return base + minIntervalMs;
            });

            long delayMs = Math.max(0, mySlot - now);
            return (delayMs == 0 ? Mono.<Long>empty() : Mono.delay(Duration.ofMillis(delayMs))).then();
        });
    }
}

