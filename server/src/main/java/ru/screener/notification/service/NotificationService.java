package ru.screener.notification.service;

import reactor.core.publisher.Mono;

public interface NotificationService {

    Mono<Void> send(Long userId, String message);
}
