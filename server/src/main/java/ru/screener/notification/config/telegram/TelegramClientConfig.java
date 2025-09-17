package ru.screener.notification.config.telegram;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class TelegramClientConfig {

    @Bean
    WebClient telegramWebClient(TelegramProperties telegramProperties) {
        ConnectionProvider provider = ConnectionProvider.builder("telegram-provider")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(58))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, telegramProperties.getTimeouts().getConnectMs())
                .responseTimeout(Duration.ofMillis(telegramProperties.getTimeouts().getReadMs()))
                .doOnConnected(connection -> connection.addHandlerLast(
                        new ReadTimeoutHandler(telegramProperties.getTimeouts().getReadMs(), TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .baseUrl(telegramProperties.getApiBaseUrl() + "/bot" + telegramProperties.getBotToken())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(clientCodecConfigurer -> {
                            clientCodecConfigurer.defaultCodecs().maxInMemorySize(telegramProperties.getHttp().getMaxInMemoryBytes());
                        })
                        .build())
                .build();
    }
}