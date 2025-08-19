package ru.screener.notification.messaging;

public interface MessageBuilder<T> {

    Class<T> getInstance();

    String buildMessage(T event);
}
