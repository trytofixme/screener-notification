package ru.screener.notification.exception.authorization;

public class UserUnauthorizedException extends RuntimeException {

    public UserUnauthorizedException(String msg) {
        super(msg);
    }
}