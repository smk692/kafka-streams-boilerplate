package com.son.boilerplate.core.annotation

import com.son.boilerplate.core.config.KafkaStreamsConfig
import com.son.boilerplate.core.enum.KafkaTopics
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafkaStreams
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@EnableKafkaStreams
@Import(KafkaStreamsConfig::class)
annotation class EnableKafkaStreamsTopology(
    val consumerGroup: String,
    val inputTopics: Array<KafkaTopics>,
    val outputTopics: Array<KafkaTopics>
)