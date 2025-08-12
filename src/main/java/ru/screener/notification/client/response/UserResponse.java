package ru.screener.notification.client.response;

import lombok.Data;

import java.util.Locale;

@Data
public class UserResponse {

    private String id;
    private String username;
    private String phoneNumber;
    private Locale locale;
}
