package ru.screener.notification;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;

import ru.screener.notification.config.TelegramProperties;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableFeignClients("ru.screener.notification.client")
@EnableKafka
@EnableRetry
@EnableConfigurationProperties(TelegramProperties.class)
public class NotificationServiceApp {
    public static void main(String[] args) {
        new SpringApplicationBuilder(NotificationServiceApp.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);
    }
}
