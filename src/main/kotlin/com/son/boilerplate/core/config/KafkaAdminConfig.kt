package com.son.boilerplate.core.config

import com.son.boilerplate.core.enum.KafkaTopics
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin

@Configuration
class KafkaAdminConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs = mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers)
        return KafkaAdmin(configs)
    }

    @Bean
    fun topicA(): NewTopic {
        return NewTopic(KafkaTopics.TOPIC_A.topic, 1, 1.toShort())
    }

    @Bean
    fun topicB(): NewTopic {
        return NewTopic(KafkaTopics.TOPIC_B.topic, 1, 1.toShort())
    }


    @Bean
    fun topicOutput1(): NewTopic {
        return NewTopic(KafkaTopics.TOPIC_OUTPUT.topic, 1, 1.toShort())
    }

}
