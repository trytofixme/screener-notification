package ru.screener.notification.client;

import feign.FeignException;
import feign.RetryableException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.screener.notification.client.response.UserResponse;
import ru.screener.notification.config.client.feign.FeignClientConfig;

@FeignClient(name = "user-service",
        url = "${services.user-service.host}:${services.user-service.port}",
        path = "/api/v1",
        configuration = FeignClientConfig.class)
public interface UserServiceClient {

    @Retryable(
            retryFor = { FeignException.class, RetryableException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @GetMapping("/users/{userId}/notification")
    UserResponse getUserById(@PathVariable String telegramId);
}