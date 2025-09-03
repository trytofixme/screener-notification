package ru.screener.notification.model.errors;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Generated;

// TODO: swagger @Schema
@JsonInclude(Include.NON_NULL)
public class ErrorResponseModel<T> implements Serializable {

    private final String code;

    private final String message;

    private T details;

    @Generated
    public ErrorResponseModel(final String code, final String message) {
        this.code = code;
        this.message = message;
    }

    @Generated
    public String getCode() {
        return this.code;
    }

    @Generated
    public String getMessage() {
        return this.message;
    }

    @Generated
    public T getDetails() {
        return this.details;
    }

    @Generated
    public void setDetails(final T details) {
        this.details = details;
    }
}