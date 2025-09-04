package com.example.kafka.streams.inventory.config

import com.example.kafka.streams.inventory.topology.InventoryTrackingTopology
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class InventoryTrackingAutoConfiguration {

    @Bean
    fun inventoryTrackingTopology(): InventoryTrackingTopology {
        return InventoryTrackingTopology()
    }

    @Bean
    fun inventoryTrackingKafkaStreams(
        inventoryTrackingTopology: InventoryTrackingTopology,
        commonStreamsProperties: Properties,
        @Value("\${kafka.topologies.inventory-tracking.application-id:inventory-tracking-app}") applicationId: String,
        @Value("\${kafka.topologies.inventory-tracking.threads:4}") numThreads: Int,
        @Value("\${kafka.topologies.inventory-tracking.state-dir:/tmp/kafka-streams/inventory-tracking}") stateDir: String
    ): KafkaStreams {
        val props = Properties().apply {
            putAll(commonStreamsProperties)
            put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
            put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, numThreads)
            put(StreamsConfig.STATE_DIR_CONFIG, stateDir)
            println("InventoryTracking KafkaStreams props: $this")
        }
        val topology = inventoryTrackingTopology.buildTopology()
        return KafkaStreams(topology, props)
    }
}