package ru.screener.notification.utils.messages;

import lombok.Generated;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {
    private static final String DEFAULT_ERROR_CODE = "unknown";
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("ru");
    private static String errorsBaseName = "errors";
    private static String textBaseName = "texts";

    public static void configure(String baseName, String baseName2) {
        errorsBaseName = baseName;
        textBaseName = baseName2;
    }

    public static String getErrorMessage(String code, Locale locale, Object... args) {
        return getMessage(errorsBaseName, code, locale, args);
    }

    public static String getErrorMessage(String code, Object... args) {
        return getMessage(errorsBaseName, code, DEFAULT_LOCALE, args);
    }

    public static String getTextMessage(String code, Locale locale, Object... args) {
        return getMessage(textBaseName, code, locale, args);
    }

    public static String getTextMessage(String code, Object... args) {
        return getMessage(textBaseName, code, DEFAULT_LOCALE, args);
    }

    public static String getMessage(String baseName, String code, Locale locale, Object... args) {
        String message;
        try {
            message = ResourceBundle.getBundle(baseName, locale).getString(code);
        } catch (MissingResourceException var8) {
            try {
                message = ResourceBundle.getBundle(baseName, locale).getString("unknown");
            } catch (MissingResourceException var7) {
                message = null;
            }
        }

        return message != null ? String.format(message, args) : null;
    }

    @Generated
    private Messages() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
