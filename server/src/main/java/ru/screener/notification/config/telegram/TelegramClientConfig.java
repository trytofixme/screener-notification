package ru.screener.notification.config.telegram;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class TelegramClientConfig {

    @Bean
    WebClient telegramWebClient(TelegramProperties props) {

        ConnectionProvider provider = ConnectionProvider.builder("tg-pool")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.ofSeconds(3))
                .pendingAcquireMaxCount(1000)
                .maxIdleTime(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS)))
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .wiretap("tg", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL);

        return WebClient.builder()
                .baseUrl(props.getApiBaseUrl() + "/bot" + props.getBotToken())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
                        .build())
                .build();
    }
}