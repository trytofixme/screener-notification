package ru.screener.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ru.screener.notification.model.BybitEvent;
import ru.screener.notification.model.enums.Direction;
import ru.screener.notification.model.enums.Strategy;

@SpringBootTest
@Testcontainers
class BybitKafkaListenerIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.2.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("telegram.bot-token", () -> "token");
        registry.add("telegram.chat-id", () -> "1");
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldSendTelegramMessage() throws Exception {
        BybitEvent event = BybitEvent.builder()
                .strategy(Strategy.RSI)
                .symbol("NOT")
                .timeframe("1H")
                .direction(Direction.UP)
                .rsi(new java.math.BigDecimal("87.8"))
                .signalNumber(3)
                .build();

        String expectedMessage = "🔴 #NOT - 1H 🔴\n🔺 RSI 87.8\n📢 За сутки: 3";

        mockServer.expect(ExpectedCount.once(),
                        MockRestRequestMatchers.requestTo("https://api.telegram.org/bottoken/sendMessage"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().json("{" +
                        "\"chat_id\":\"1\"," +
                        "\"text\":\"" + expectedMessage + "\"}"))
                .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("notification.send", payload).get();

        Thread.sleep(1000);
        mockServer.verify();
    }
}
