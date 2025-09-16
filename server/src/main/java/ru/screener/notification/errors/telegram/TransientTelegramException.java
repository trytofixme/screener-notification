package ru.screener.notification.errors.telegram;

public class TransientTelegramException extends RuntimeException {

    public TransientTelegramException(String message) {
        super(message);
    }

    public TransientTelegramException(String message, Throwable cause) {
        super(message, cause);
    }
}
