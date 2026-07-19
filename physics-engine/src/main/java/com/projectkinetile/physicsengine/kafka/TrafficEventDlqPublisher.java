package com.projectkinetile.physicsengine.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.config.KafkaProducerConfig;

/**
 * Forwards poison or invalid Kafka messages to a dead-letter topic for inspection.
 */
@Component
public class TrafficEventDlqPublisher {

  private static final Logger log = LoggerFactory.getLogger(TrafficEventDlqPublisher.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final String dlqTopic;

  public TrafficEventDlqPublisher(
      @Qualifier(KafkaProducerConfig.DLQ_KAFKA_TEMPLATE_BEAN)
          KafkaTemplate<String, String> kafkaTemplate,
      AppSecurityProperties appSecurityProperties) {
    this.kafkaTemplate = kafkaTemplate;
    this.dlqTopic = appSecurityProperties.getKafka().getTopics().getRawTrafficDlq();
  }

  /**
   * Sends the original payload to the DLQ with a reason header encoded in the key suffix.
   *
   * @param originalKey partition key from source record
   * @param payload raw JSON value
   * @param reason short failure reason (no PII)
   */
  public void publish(String originalKey, String payload, String reason) {
    String key = (originalKey != null ? originalKey : "unknown") + "|" + reason;
    try {
      kafkaTemplate.send(new ProducerRecord<>(dlqTopic, key, payload));
    } catch (Exception e) {
      log.warn("Failed to publish message to DLQ topic {}: {}", dlqTopic, e.getMessage());
    }
  }
}
