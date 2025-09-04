package com.example.kafka.streams.order.config

import com.example.kafka.streams.order.topology.OrderProcessingTopology
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class OrderProcessingAutoConfiguration {

    @Bean
    fun orderProcessingTopology(): OrderProcessingTopology {
        return OrderProcessingTopology()
    }

    @Bean
    fun orderProcessingKafkaStreams(
        orderProcessingTopology: OrderProcessingTopology,
        commonStreamsProperties: Properties,
        @Value("\${kafka.topologies.order-processing.application-id:order-processing-app}") applicationId: String,
        @Value("\${kafka.topologies.order-processing.threads:4}") numThreads: Int,
        @Value("\${kafka.topologies.order-processing.state-dir:/tmp/kafka-streams/order-processing}") stateDir: String
    ): KafkaStreams {
        val props = Properties().apply {
            putAll(commonStreamsProperties)
            put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
            put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, numThreads)
            put(StreamsConfig.STATE_DIR_CONFIG, stateDir)
            println("OrderProcessing KafkaStreams props: $this")
        }
        val topology = orderProcessingTopology.buildTopology()
        return KafkaStreams(topology, props)
    }
}