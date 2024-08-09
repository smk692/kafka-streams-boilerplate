package com.son.boilerplate.core.annotation

import org.springframework.stereotype.Component

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class KafkaStreamsTopology(val name: String)