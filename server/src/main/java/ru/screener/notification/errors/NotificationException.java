package ru.screener.notification.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import ru.screener.notification.model.errors.ErrorType;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class NotificationException extends RuntimeException {

    private final String code;
    private final ErrorType errorType;
    private final HttpStatus httpStatus;

}
