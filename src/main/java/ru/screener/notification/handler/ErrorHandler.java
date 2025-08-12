package ru.screener.notification.handler;

@FunctionalInterface
public interface ErrorHandler {

    String handle(Exception ex);
}
