package ru.screener.notification.service;

import ru.screener.notification.client.response.UserResponse;

public interface NotificationService {

    void send(UserResponse user, String message);
}
