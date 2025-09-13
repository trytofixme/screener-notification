package ru.screener.notification.service;

public interface NotificationService {

    void send(Long userId, String message);
}
