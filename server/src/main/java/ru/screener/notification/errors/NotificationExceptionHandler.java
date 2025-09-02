package ru.screener.notification.errors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.screener.notification.model.errors.ErrorResponseModel;
import ru.screener.notification.model.errors.ErrorType;
import ru.screener.notification.utils.errors.RequestContextHelper;

import java.util.Locale;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static ru.screener.notification.model.errors.ErrorType.INNER;
import static ru.screener.notification.utils.messages.Messages.getErrorMessage;

@Slf4j
@RestControllerAdvice
public class NotificationExceptionHandler {

    private static final String INTERNAL_ERROR_CODE = "internal_error";

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponseModel<?> handle(Exception e) {
        log.error(e.getMessage(), e);
        return coreHandle(INTERNAL_ERROR_CODE, INNER);
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(NotificationException.class)
    public ErrorResponseModel<?> handle(NotificationException e) {
        log.error(e.getMessage(), e);
        return coreHandle(e.getCode(), e.getErrorType());
    }

    private ErrorResponseModel<?> coreHandle(String code, ErrorType errorType) {
        RequestContextHelper.setResponseScreenerError(errorType, code);
        return new ErrorResponseModel<>(code, getErrorMessage(code, Locale.forLanguageTag("ru")));
    }
}
