package com.example.kafka.streams.app.admin.model

import org.apache.kafka.streams.KafkaStreams
import java.time.Instant

data class TopologyState(
    val name: String,
    val state: KafkaStreams.State,
    val previousState: KafkaStreams.State? = null,
    val lastUpdated: Instant,
    val metadata: Map<String, Any> = emptyMap()
)

data class TopologyStatus(
    val name: String,
    val applicationId: String,
    val state: KafkaStreams.State,
    val metadata: List<ThreadInfo>,
    val lastUpdated: Instant,
    val isHealthy: Boolean = state == KafkaStreams.State.RUNNING
)

data class ThreadInfo(
    val threadName: String,
    val threadState: String,
    val activeTasks: List<String>,
    val standbyTasks: List<String>
)

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val timestamp: String = java.time.Instant.now().toString()
)

data class HealthResponse(
    val name: String,
    val applicationId: String,
    val status: String,
    val healthy: Boolean,
    val lastUpdated: java.time.Instant?,
    val threadCount: Int,
    val details: Map<String, Any> = emptyMap()
)