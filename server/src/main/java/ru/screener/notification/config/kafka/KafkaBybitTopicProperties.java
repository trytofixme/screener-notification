package ru.screener.notification.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.data.kafka.topic.notification")
@Configuration
public class KafkaBybitTopicProperties {

    private String name;
    private int partitions;
}
