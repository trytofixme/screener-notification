package ru.screener.notification.errors.telegram;

public class PermanentTelegramException extends RuntimeException {

    public PermanentTelegramException(String msg) { super(msg); }
}
