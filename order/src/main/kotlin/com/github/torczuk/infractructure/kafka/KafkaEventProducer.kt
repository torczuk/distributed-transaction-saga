package com.github.torczuk.infractructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.torczuk.domain.EventProducer
import com.github.torczuk.domain.OrderEvent
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class KafkaEventProducer(config: ProducerConfiguration,
                         private val topic: String,
                         val objectMapper: ObjectMapper) : EventProducer<OrderEvent> {

    val producer = KafkaProducer<String, String>(config.properties())
    val log = LoggerFactory.getLogger(KafkaEventProducer::class.java)

    override fun publish(event: OrderEvent) {
        producer.send(ProducerRecord(topic, event.transaction, objectMapper.writeValueAsString(event)))
        log.info("published on {}:  {}", topic, event);
    }
}