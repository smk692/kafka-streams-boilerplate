package com.son.boilerplate.core.config

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.StreamsBuilderFactoryBean

@Configuration
class KafkaStreamsConfig {

    @Value("\${spring.application.name}")
    private lateinit var applicationName: String

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.streams.state-dir}")
    private lateinit var stateDir: String

    @Bean(name = ["customKafkaStreamsBuilder"])
    fun customKafkaStreamsBuilder(): StreamsBuilderFactoryBean {
        val props = mapOf(
            StreamsConfig.APPLICATION_ID_CONFIG to applicationName,
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            StreamsConfig.STATE_DIR_CONFIG to stateDir
        )
        val config = KafkaStreamsConfiguration(props)
        return StreamsBuilderFactoryBean(config)
    }

    @Bean
    fun streamsBuilder(customKafkaStreamsBuilder: StreamsBuilderFactoryBean): StreamsBuilder {
        return customKafkaStreamsBuilder.getObject()
    }
}