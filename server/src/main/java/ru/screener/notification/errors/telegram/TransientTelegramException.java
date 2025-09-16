package ru.screener.notification.errors.telegram;

public class TransientTelegramException extends RuntimeException {

    public TransientTelegramException(String message) {
        super(message);
    }
}
