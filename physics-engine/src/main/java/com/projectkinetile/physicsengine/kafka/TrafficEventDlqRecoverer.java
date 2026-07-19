package com.projectkinetile.physicsengine.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends unrecoverable consumer failures to the traffic DLQ via {@link TrafficEventDlqPublisher}.
 */
@Component
public class TrafficEventDlqRecoverer implements org.springframework.kafka.listener.ConsumerRecordRecoverer {

  private static final Logger log = LoggerFactory.getLogger(TrafficEventDlqRecoverer.class);

  private final TrafficEventDlqPublisher dlqPublisher;

  public TrafficEventDlqRecoverer(TrafficEventDlqPublisher dlqPublisher) {
    this.dlqPublisher = dlqPublisher;
  }

  @Override
  public void accept(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
    if (log.isWarnEnabled()) {
      log.warn(
          "Kafka record sent to DLQ after retries: topic={} partition={} offset={} ({})",
          consumerRecord.topic(),
          consumerRecord.partition(),
          consumerRecord.offset(),
          exception.toString());
    }
    String payload = consumerRecord.value() != null ? consumerRecord.value().toString() : "";
    String key = consumerRecord.key() != null ? consumerRecord.key().toString() : null;
    String reason = "processing_error:" + exception.getClass().getSimpleName();
    dlqPublisher.publish(key, payload, reason);
  }
}
