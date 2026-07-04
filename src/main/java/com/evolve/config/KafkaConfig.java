package com.evolve.config;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaConfig {

	@Bean
	public <T> ConcurrentKafkaListenerContainerFactory<String, T> kafkaListenerContainerFactory(
			ConsumerFactory<String, Object> consumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
		return factory;
	}

	@Bean
	public <T> ProducerFactory<String, T> producerFactory(KafkaProperties kafkaProperties) {
		return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
	}

	@Bean
	public <T> KafkaTemplate<String, T> kafkaTemplate(ProducerFactory<String, T> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	public <T> ConsumerFactory<String, T> consumerFactory(KafkaProperties kafkaProperties) {
		return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
	}
}
