package com.example.kafka.streams.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = [
    "com.example.kafka.streams.app",
    "com.example.kafka.streams.order", 
    "com.example.kafka.streams.inventory"
])
class KafkaStreamsApplication

fun main(args: Array<String>) {
    runApplication<KafkaStreamsApplication>(*args)
}