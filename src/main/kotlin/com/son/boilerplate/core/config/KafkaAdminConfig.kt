package com.son.boilerplate.core.config

import com.son.boilerplate.core.enum.AnotherTopologyTopics
import com.son.boilerplate.core.enum.StateStores
import com.son.boilerplate.core.enum.WordCountTopics
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

    /**
     * INPUT topics
     */
    @Bean
    fun inTopicA(): NewTopic {
        return NewTopic(AnotherTopologyTopics.INPUT_1.topic, 1, 3.toShort())
    }

    @Bean
    fun inTopicC(): NewTopic {
        return NewTopic(WordCountTopics.INPUT.topic, 1, 3.toShort())
    }

    /**
     * OUTPUT topics
     */
    @Bean
    fun outTopicA(): NewTopic {
        return NewTopic(AnotherTopologyTopics.OUTPUT.topic, 1, 3.toShort())
    }

    @Bean
    fun outTopicB(): NewTopic {
        return NewTopic(WordCountTopics.OUTPUT.topic, 1, 3.toShort())
    }

    /**
     * Repartition and State Store topics for WordCountProcessor and Custom Aggregations
     */
    @Bean
    fun wordCountRepartitionTopic(): NewTopic {
        return NewTopic("${StateStores.WORD_COUNT_STORE.store}-repartition", 1, 3.toShort())
    }

    @Bean
    fun wordCountStateStoreTopic(): NewTopic {
        return NewTopic("${StateStores.WORD_COUNT_STORE.store}-changelog", 1, 3.toShort())
    }

    @Bean
    fun customRepartitionTopic(): NewTopic {
        return NewTopic("${StateStores.CUSTOM_AGGREGATE_STORE.store}-repartition", 1, 3.toShort())
    }

    @Bean
    fun customStateStoreTopic(): NewTopic {
        return NewTopic("${StateStores.CUSTOM_AGGREGATE_STORE.store}-changelog", 1, 3.toShort())
    }
}