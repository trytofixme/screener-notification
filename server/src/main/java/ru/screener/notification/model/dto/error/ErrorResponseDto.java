package ru.screener.notification.model.dto.error;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ErrorResponseDto {

    private String errorMsg;
    private LocalDateTime timestamp;
    private int codeResponse;
}
