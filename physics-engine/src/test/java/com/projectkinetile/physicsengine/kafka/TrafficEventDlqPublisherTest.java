package com.projectkinetile.physicsengine.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.projectkinetile.physicsengine.config.AppSecurityProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("Traffic event DLQ publisher")
class TrafficEventDlqPublisherTest {

  @Mock private KafkaTemplate<String, String> kafkaTemplate;

  private TrafficEventDlqPublisher publisher;

  @BeforeEach
  void setUp() {
    AppSecurityProperties props = new AppSecurityProperties();
    props.getKafka().getTopics().setRawTrafficDlq("raw-traffic-events-dlq");
    publisher = new TrafficEventDlqPublisher(kafkaTemplate, props);
  }

  @Test
  @DisplayName("publishes payload with reason encoded in the key")
  void publish_encodesReasonInKey() {
    publisher.publish("tile-1", "{\"event\":true}", "validation_failed");

    verify(kafkaTemplate)
        .send(
            new ProducerRecord<>(
                "raw-traffic-events-dlq", "tile-1|validation_failed", "{\"event\":true}"));
  }

  @Test
  @DisplayName("uses unknown key prefix when original key is null")
  void publish_nullKey_usesUnknownPrefix() {
    publisher.publish(null, "{}", "json_parse_error");

    verify(kafkaTemplate)
        .send(new ProducerRecord<>("raw-traffic-events-dlq", "unknown|json_parse_error", "{}"));
  }

  @Test
  @DisplayName("uses DLQ topic from application configuration")
  void publish_usesConfiguredDlqTopic() {
    AppSecurityProperties props = new AppSecurityProperties();
    props.getKafka().getTopics().setRawTrafficDlq("projectkinetile-dlq");
    TrafficEventDlqPublisher configuredPublisher =
        new TrafficEventDlqPublisher(kafkaTemplate, props);

    configuredPublisher.publish("tile-9", "{}", "validation_failed");

    verify(kafkaTemplate)
        .send(new ProducerRecord<>("projectkinetile-dlq", "tile-9|validation_failed", "{}"));
  }

  @Test
  @DisplayName("preserves empty original key without substituting unknown")
  void publish_emptyKey_doesNotUseUnknownPrefix() {
    publisher.publish("", "{}", "validation_failed");

    verify(kafkaTemplate)
        .send(new ProducerRecord<>("raw-traffic-events-dlq", "|validation_failed", "{}"));
  }

  @Test
  @DisplayName("does not throw when Kafka send fails")
  void publish_sendFailure_isSwallowed() {
    doThrow(new RuntimeException("broker down"))
        .when(kafkaTemplate)
        .send(
            new ProducerRecord<>(
                "raw-traffic-events-dlq", "k|processing_error:RuntimeException", "{}"));

    assertThatCode(() -> publisher.publish("k", "{}", "processing_error:RuntimeException"))
        .doesNotThrowAnyException();
  }
}
