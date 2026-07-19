package com.projectkinetile.physicsengine.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.lang.NonNull;

/**
 * Kafka producer beans used for DLQ forwarding of invalid traffic events.
 */
@Configuration
public class KafkaProducerConfig {

  /** Spring bean name for the DLQ {@link ProducerFactory} (String key/value serializers). */
  public static final String DLQ_PRODUCER_FACTORY_BEAN = "dlqProducerFactory";

  /** Spring bean name for the DLQ {@link KafkaTemplate}. */
  public static final String DLQ_KAFKA_TEMPLATE_BEAN = "dlqKafkaTemplate";

  /**
   * Producer factory for DLQ records (String key/value serializers).
   *
   * @param kafkaProperties Spring Kafka properties
   * @return producer factory for {@link #dlqKafkaTemplate}
   */
  @Bean(name = DLQ_PRODUCER_FACTORY_BEAN)
  public ProducerFactory<String, String> dlqProducerFactory(KafkaProperties kafkaProperties) {
    var props = kafkaProperties.buildProducerProperties(null);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

  /**
   * Template for publishing invalid traffic events to the DLQ topic.
   *
   * @param dlqProducerFactory qualified DLQ producer factory
   * @return kafka template for {@link com.projectkinetile.physicsengine.kafka.TrafficEventDlqPublisher}
   */
  @Bean(name = DLQ_KAFKA_TEMPLATE_BEAN)
  public KafkaTemplate<String, String> dlqKafkaTemplate(
      @Qualifier(DLQ_PRODUCER_FACTORY_BEAN) @NonNull
      ProducerFactory<String, String> dlqProducerFactory) {
    return new KafkaTemplate<>(dlqProducerFactory);
  }
}
