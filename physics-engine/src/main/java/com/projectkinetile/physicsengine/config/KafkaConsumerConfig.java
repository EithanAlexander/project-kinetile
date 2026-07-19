package com.projectkinetile.physicsengine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.projectkinetile.physicsengine.kafka.TrafficEventDlqRecoverer;

/** Kafka listener error handling with bounded retries and DLQ recovery. */
@Configuration
public class KafkaConsumerConfig {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

  @Bean
  public DefaultErrorHandler kafkaErrorHandler(
      TrafficEventDlqRecoverer recoverer, AppSecurityProperties appSecurityProperties) {
    int maxRetries = Math.max(0, appSecurityProperties.getKafka().getConsumer().getErrorMaxRetries());
    long backoffMs = Math.max(0L, appSecurityProperties.getKafka().getConsumer().getErrorBackoffMs());
    FixedBackOff backOff = new FixedBackOff(backoffMs, maxRetries);
    DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
    handler.setCommitRecovered(true);
    handler.setRetryListeners(
        (consumerRecord, exception, deliveryAttempt) -> {
          if (log.isWarnEnabled()) {
            log.warn(
                "Kafka consumer failed (attempt {} of {}): topic={} partition={} offset={} ({})",
                deliveryAttempt,
                maxRetries + 1,
                consumerRecord.topic(),
                consumerRecord.partition(),
                consumerRecord.offset(),
                exception.toString());
          }
        });
    return handler;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      @NonNull ConsumerFactory<String, String> consumerFactory,
      @NonNull DefaultErrorHandler kafkaErrorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(kafkaErrorHandler);
    return factory;
  }
}
