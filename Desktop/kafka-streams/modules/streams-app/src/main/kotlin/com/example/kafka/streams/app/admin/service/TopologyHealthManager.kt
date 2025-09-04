package com.example.kafka.streams.app.admin.service

import com.example.kafka.streams.app.admin.model.TopologyState
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import mu.KotlinLogging
import org.apache.kafka.streams.KafkaStreams
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class TopologyHealthManager(
    private val meterRegistry: MeterRegistry
) {
    private val logger = KotlinLogging.logger {}
    private val topologyStates = ConcurrentHashMap<String, TopologyState>() // ApplicationId -> TopologyState
    
    fun monitorTopology(applicationId: String, streams: KafkaStreams) {
        streams.setStateListener { newState, oldState ->
            val topologyName = extractTopologyName(applicationId)
            val topologyState = TopologyState(
                name = topologyName,
                state = newState,
                previousState = oldState,
                lastUpdated = Instant.now()
            )
            
            topologyStates[applicationId] = topologyState
            
            // 메트릭 수집 (applicationId 및 topology name 모두 포함)
            meterRegistry.gauge(
                "kafka.streams.topology.state", 
                Tags.of("applicationId", applicationId, "topology", topologyName, "state", newState.name), 
                newState.ordinal.toDouble()
            )
            
            logger.info { "Topology $applicationId ($topologyName) state changed: $oldState -> $newState" }
            
            when (newState) {
                KafkaStreams.State.ERROR -> handleTopologyError(applicationId, streams)
                KafkaStreams.State.RUNNING -> handleTopologyRunning(applicationId)
                KafkaStreams.State.REBALANCING -> handleTopologyRebalancing(applicationId)
                KafkaStreams.State.PENDING_SHUTDOWN -> logger.info { "Topology $applicationId is shutting down" }
                KafkaStreams.State.NOT_RUNNING -> logger.info { "Topology $applicationId is not running" }
                KafkaStreams.State.CREATED -> logger.info { "Topology $applicationId has been created" }
                else -> logger.debug { "Topology $applicationId in state: $newState" }
            }
        }
    }
    
    private fun handleTopologyError(applicationId: String, streams: KafkaStreams) {
        val topologyName = extractTopologyName(applicationId)
        logger.error { "Topology $applicationId ($topologyName) entered ERROR state" }
        
        // 에러 메트릭 증가
        meterRegistry.counter(
            "kafka.streams.topology.errors", 
            Tags.of("applicationId", applicationId, "topology", topologyName)
        ).increment()
        
        // 알림 또는 자동 복구 로직을 여기에 추가할 수 있습니다
        // notificationService.sendAlert("Topology $applicationId failed")
    }
    
    private fun handleTopologyRunning(applicationId: String) {
        val topologyName = extractTopologyName(applicationId)
        logger.info { "Topology $applicationId ($topologyName) is now running successfully" }
        
        // 복구 메트릭
        meterRegistry.counter(
            "kafka.streams.topology.recoveries", 
            Tags.of("applicationId", applicationId, "topology", topologyName)
        ).increment()
    }
    
    private fun handleTopologyRebalancing(applicationId: String) {
        val topologyName = extractTopologyName(applicationId)
        logger.info { "Topology $applicationId ($topologyName) is rebalancing" }
        
        // 리밸런싱 메트릭
        meterRegistry.counter(
            "kafka.streams.topology.rebalances", 
            Tags.of("applicationId", applicationId, "topology", topologyName)
        ).increment()
    }
    
    fun getTopologyState(applicationId: String): TopologyState? = topologyStates[applicationId]
    
    fun getAllTopologyStates(): Map<String, TopologyState> = topologyStates.toMap()
    
    fun getHealthyTopologies(): List<String> {
        return topologyStates.entries
            .filter { it.value.state == KafkaStreams.State.RUNNING }
            .map { it.key } // ApplicationId 반환
    }
    
    fun getUnhealthyTopologies(): List<String> {
        return topologyStates.entries
            .filter { it.value.state in listOf(KafkaStreams.State.ERROR, KafkaStreams.State.NOT_RUNNING) }
            .map { it.key } // ApplicationId 반환
    }
    
    private fun extractTopologyName(applicationId: String): String {
        // applicationId에서 토폴로지 이름을 추출 (예: "order-processing-app" -> "order-processing")
        return applicationId.replace("-app", "")
    }
}