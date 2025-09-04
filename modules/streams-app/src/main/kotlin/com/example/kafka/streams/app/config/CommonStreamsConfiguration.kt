package com.example.kafka.streams.app.config

import org.apache.kafka.streams.StreamsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class CommonStreamsConfiguration {

    @Bean
    fun commonStreamsProperties(
        @Value("\${kafka.bootstrap-servers}") bootstrapServers: String
    ): Properties {
        return createCommonStreamsProperties(bootstrapServers)
    }
    
    fun createCommonStreamsProperties(bootstrapServers: String): Properties {
        return Properties().apply {
            put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, org.apache.kafka.common.serialization.Serdes.String()::class.java)
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, org.apache.kafka.common.serialization.Serdes.String()::class.java)
            put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2)
            put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000)
            put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, 
                org.apache.kafka.streams.errors.LogAndContinueExceptionHandler::class.java)
        }
    }
}