package ru.screener.notification.config.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TelegramRateGate {

    private final TelegramProperties props;

    // Очередь «разрешений» на отправку
    private final Flux<Long> permits;

    public TelegramRateGate(TelegramProperties props) {
        int rps = Math.max(1, props.getMaxGlobalRps());
        // Раздаём разрешения равномерно по времени
        long periodMs = Math.max(1, 1000 / rps);
        this.permits = Flux.interval(Duration.ofMillis(periodMs)).onBackpressureBuffer(10000);
        this.props = props;
    }

    /** Пропускаем одну отправку только когда придёт «разрешение» */
    public <T> Mono<T> gate(Mono<T> singleSend) {
        return permits.next().then(singleSend);
    }
}

