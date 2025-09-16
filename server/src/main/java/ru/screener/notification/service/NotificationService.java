package ru.screener.notification.service;

import reactor.core.publisher.Mono;

public interface NotificationService {

    Mono<Void> sendAsync(Long chatId, String text);
}
