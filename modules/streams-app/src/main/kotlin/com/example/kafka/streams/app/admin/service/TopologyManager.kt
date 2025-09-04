package com.example.kafka.streams.app.admin.service

import com.example.kafka.streams.app.admin.model.TopologyStatus
import com.example.kafka.streams.app.admin.model.ThreadInfo
import mu.KotlinLogging
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Component
class TopologyManager(
    private val kafkaStreamsMap: Map<String, KafkaStreams>, // Spring이 자동으로 모든 KafkaStreams Bean들을 주입
    private val healthManager: TopologyHealthManager
) {
    private val logger = KotlinLogging.logger {}
    private val beanNameToApplicationIdMap = ConcurrentHashMap<String, String>()
    private val runningStreams = ConcurrentHashMap<String, KafkaStreams>()
    
    @PostConstruct
    fun startTopologies() {
        logger.info { "Starting topology manager with ${kafkaStreamsMap.size} configured KafkaStreams" }
        
        kafkaStreamsMap.forEach { (beanName, streams) ->
            try {
                val applicationId = mapBeanNameToApplicationId(beanName)
                beanNameToApplicationIdMap[beanName] = applicationId
                
                startKafkaStreams(applicationId, streams)
                logger.info { "Successfully started topology: $beanName (applicationId: $applicationId)" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to start topology: $beanName" }
            }
        }
    }
    
    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down topology manager..." }
        
        runningStreams.forEach { (applicationId, streams) ->
            try {
                logger.info { "Stopping topology: $applicationId" }
                streams.close(Duration.ofSeconds(30))
                logger.info { "Successfully stopped topology: $applicationId" }
            } catch (e: Exception) {
                logger.error(e) { "Error stopping topology: $applicationId" }
            }
        }
        
        runningStreams.clear()
        logger.info { "Topology manager shutdown completed" }
    }
    
    private fun startKafkaStreams(applicationId: String, streams: KafkaStreams) {
        logger.info { "Starting KafkaStreams: $applicationId" }
        
        // 예외 처리 설정
        streams.setUncaughtExceptionHandler { thread, exception ->
            logger.error(exception) { "Uncaught exception in $applicationId on thread $thread" }
            handleTopologyError(applicationId, streams, exception)
            StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
        }
        
        // 상태 리스너 설정
        healthManager.monitorTopology(applicationId, streams)
        
        // 이전에 실행 중인 인스턴스가 있다면 정리
        stopTopologyByApplicationId(applicationId)
        
        runningStreams[applicationId] = streams
        streams.start()
        
        logger.info { "KafkaStreams $applicationId started successfully" }
    }
    
    fun stopTopology(applicationId: String): Boolean {
        return stopTopologyByApplicationId(applicationId)
    }
    
    fun stopTopologyByApplicationId(applicationId: String): Boolean {
        return runningStreams[applicationId]?.let { streams ->
            try {
                logger.info { "Stopping topology instance: $applicationId" }
                streams.close(Duration.ofSeconds(30))
                runningStreams.remove(applicationId)
                logger.info { "Successfully stopped topology instance: $applicationId" }
                true
            } catch (e: Exception) {
                logger.error(e) { "Failed to stop topology instance: $applicationId" }
                false
            }
        } ?: run {
            logger.debug { "Topology instance $applicationId is not running" }
            false
        }
    }
    
    fun restartTopology(applicationId: String): Boolean {
        logger.info { "Restarting topology: $applicationId" }
        
        return if (stopTopology(applicationId)) {
            kafkaStreamsMap[applicationId]?.let { streams ->
                try {
                    startKafkaStreams(applicationId, streams)
                    logger.info { "Successfully restarted topology: $applicationId" }
                    true
                } catch (e: Exception) {
                    logger.error(e) { "Failed to restart topology: $applicationId" }
                    false
                }
            } ?: run {
                logger.error { "No KafkaStreams found for applicationId: $applicationId" }
                false
            }
        } else {
            logger.error { "Failed to stop topology $applicationId, cannot restart" }
            false
        }
    }
    
    fun getTopologyStatus(applicationId: String): TopologyStatus? {
        return runningStreams[applicationId]?.let { streams ->
            TopologyStatus(
                name = extractTopologyName(applicationId),
                applicationId = applicationId,
                state = streams.state(),
                metadata = streams.metadataForLocalThreads().map { thread ->
                    ThreadInfo(
                        threadName = thread.threadName(),
                        threadState = thread.threadState(),
                        activeTasks = thread.activeTasks().map { it.toString() },
                        standbyTasks = thread.standbyTasks().map { it.toString() }
                    )
                },
                lastUpdated = Instant.now()
            )
        }
    }
    
    fun getTopologyStatusByApplicationId(applicationId: String): TopologyStatus? {
        return runningStreams[applicationId]?.let { streams ->
            TopologyStatus(
                name = extractTopologyName(applicationId),
                applicationId = applicationId,
                state = streams.state(),
                metadata = streams.metadataForLocalThreads().map { thread ->
                    ThreadInfo(
                        threadName = thread.threadName(),
                        threadState = thread.threadState(),
                        activeTasks = thread.activeTasks().map { it.toString() },
                        standbyTasks = thread.standbyTasks().map { it.toString() }
                    )
                },
                lastUpdated = Instant.now()
            )
        }
    }
    
    fun getAllTopologyStatuses(): Map<String, TopologyStatus> {
        return runningStreams.mapValues { (applicationId, streams) ->
            TopologyStatus(
                name = extractTopologyName(applicationId),
                applicationId = applicationId,
                state = streams.state(),
                metadata = streams.metadataForLocalThreads().map { thread ->
                    ThreadInfo(
                        threadName = thread.threadName(),
                        threadState = thread.threadState(),
                        activeTasks = thread.activeTasks().map { it.toString() },
                        standbyTasks = thread.standbyTasks().map { it.toString() }
                    )
                },
                lastUpdated = Instant.now()
            )
        }
    }
    
    fun isTopologyRunning(applicationId: String): Boolean {
        return runningStreams[applicationId]?.state() == KafkaStreams.State.RUNNING
    }
    
    private fun extractTopologyName(applicationId: String): String {
        // applicationId에서 토폴로지 이름을 추출 (예: "order-processing-app" -> "order-processing")
        return applicationId.replace("-app", "")
    }
    
    private fun mapBeanNameToApplicationId(beanName: String): String {
        // Bean 이름을 ApplicationId로 매핑
        return when (beanName) {
            "orderProcessingKafkaStreams" -> "order-processing-app"
            "inventoryTrackingKafkaStreams" -> "inventory-tracking-app"
            else -> {
                // Bean 이름에서 유추 (예: "someTopologyKafkaStreams" -> "some-topology-app")
                val topologyName = beanName
                    .removeSuffix("KafkaStreams")
                    .replace(Regex("([a-z])([A-Z])"), "$1-$2")
                    .lowercase()
                "$topologyName-app"
            }
        }
    }
    
    private fun handleTopologyError(applicationId: String, streams: KafkaStreams, exception: Throwable) {
        logger.error(exception) { "Handling error for topology $applicationId" }
        
        // 재시작이 필요한 경우 비동기로 처리
        if (shouldRestartOnError(exception)) {
            logger.info { "Scheduling restart for topology $applicationId due to: ${exception::class.simpleName}" }
            CompletableFuture.runAsync {
                try {
                    Thread.sleep(5000) // 5초 대기 후 재시작
                    logger.info { "Attempting to restart topology $applicationId after error" }
                    restartTopology(applicationId)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to restart topology $applicationId after error" }
                }
            }
        }
    }
    
    private fun shouldRestartOnError(exception: Throwable): Boolean {
        // 재시작이 필요한 예외 타입 판단
        return when (exception) {
            is org.apache.kafka.streams.errors.StreamsException -> true
            is org.apache.kafka.common.errors.SerializationException -> false // 데이터 문제는 재시작으로 해결되지 않음
            is org.apache.kafka.common.KafkaException -> true
            else -> false
        }
    }
}