package ru.screener.notification.utils.errors;

import org.springframework.web.context.request.RequestContextHolder;
import ru.screener.notification.model.errors.ErrorType;

import java.util.Optional;

public final class RequestContextHelper {

    public static String RESPONSE_ERROR_TYPE = "screener.response.errorType";
    public static String RESPONSE_ERROR_ID = "screener.response.errorId";

    public static void setResponseScreenerError(ErrorType errorType, String errorId) {
        Optional.ofNullable(RequestContextHolder.getRequestAttributes()).ifPresent((ra) -> ra.setAttribute(RESPONSE_ERROR_TYPE, errorType, 0));
        Optional.ofNullable(RequestContextHolder.getRequestAttributes()).ifPresent((ra) -> ra.setAttribute(RESPONSE_ERROR_ID, errorId, 0));
    }
}
