package ru.screener.notification.service;

import reactor.core.publisher.Mono;

public interface NotificationService {

    void send(Long userId, String message);
}
