package net.lctafrica.claimsapis.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory


@EnableKafka
@Configuration
class KafkaConsumerConfig {
	@Bean
	fun consumerFactory(): ConsumerFactory<String, String> {
		val props: MutableMap<String, Any> = HashMap()
		props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "http://localhost:9092"
		props[ConsumerConfig.GROUP_ID_CONFIG] = "benefit-topic-consumer1"
		props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
		props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
		return DefaultKafkaConsumerFactory(props)
	}

	@Bean
	fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
		val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
		factory.setConsumerFactory(consumerFactory())
		return factory
	}
}