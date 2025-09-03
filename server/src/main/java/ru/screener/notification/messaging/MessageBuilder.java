package ru.screener.notification.messaging;

public interface MessageBuilder<T> {

    String buildMessage(T event);
}
