package ru.screener.notification;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import ru.screener.notification.config.telegram.TelegramProperties;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableKafka
@EnableConfigurationProperties(TelegramProperties.class)
public class App {
    public static void main(String[] args) {
        new SpringApplicationBuilder(App.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);
    }
}
