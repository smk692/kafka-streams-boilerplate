package com.son.boilerplate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaStreamsBoilerplateApplication

fun main(args: Array<String>) {
    runApplication<KafkaStreamsBoilerplateApplication>(*args)
}
