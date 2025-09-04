package com.example.kafka.streams.app.admin.controller

import com.example.kafka.streams.app.admin.service.TopologyHealthManager
import com.example.kafka.streams.app.admin.service.TopologyManager
import com.example.kafka.streams.app.admin.model.TopologyStatus
import com.example.kafka.streams.app.admin.model.ApiResponse
import com.example.kafka.streams.app.admin.model.HealthResponse
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/topologies")
class TopologyAdminController(
    private val topologyManager: TopologyManager,
    private val healthManager: TopologyHealthManager
) {
    
    private val logger = KotlinLogging.logger {}
    
    @GetMapping
    fun getAllTopologies(): ResponseEntity<Map<String, Any>> {
        logger.info { "Getting all topologies status" }
        
        val states = healthManager.getAllTopologyStates()
        val statuses = topologyManager.getAllTopologyStatuses()
        
        val response = mapOf(
            "topologies" to statuses,
            "states" to states,
            "summary" to mapOf(
                "total" to states.size,
                "running" to healthManager.getHealthyTopologies().size,
                "unhealthy" to healthManager.getUnhealthyTopologies().size
            )
        )
        
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/{applicationId}")
    fun getTopologyStatus(@PathVariable applicationId: String): ResponseEntity<TopologyStatus> {
        logger.info { "Getting status for topology: $applicationId" }
        
        val status = topologyManager.getTopologyStatus(applicationId)
        return if (status != null) {
            ResponseEntity.ok(status)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{applicationId}/stop")
    fun stopTopology(@PathVariable applicationId: String): ResponseEntity<ApiResponse> {
        logger.info { "Request to stop topology: $applicationId" }
        
        return try {
            val success = topologyManager.stopTopology(applicationId)
            if (success) {
                ResponseEntity.ok(ApiResponse(true, "Topology $applicationId stopped successfully"))
            } else {
                ResponseEntity.badRequest().body(
                    ApiResponse(false, "Failed to stop topology $applicationId or topology not found")
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error stopping topology $applicationId" }
            ResponseEntity.internalServerError().body(
                ApiResponse(false, "Error stopping topology: ${e.message}")
            )
        }
    }
    
    @PostMapping("/{applicationId}/restart")
    fun restartTopology(@PathVariable applicationId: String): ResponseEntity<ApiResponse> {
        logger.info { "Request to restart topology: $applicationId" }
        
        return try {
            val success = topologyManager.restartTopology(applicationId)
            if (success) {
                ResponseEntity.ok(ApiResponse(true, "Topology $applicationId restarted successfully"))
            } else {
                ResponseEntity.badRequest().body(
                    ApiResponse(false, "Failed to restart topology $applicationId")
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error restarting topology $applicationId" }
            ResponseEntity.internalServerError().body(
                ApiResponse(false, "Error restarting topology: ${e.message}")
            )
        }
    }
    
    @GetMapping("/{applicationId}/health")
    fun getTopologyHealth(@PathVariable applicationId: String): ResponseEntity<HealthResponse> {
        logger.debug { "Getting health for topology: $applicationId" }
        
        val state = healthManager.getTopologyState(applicationId)
        val status = topologyManager.getTopologyStatus(applicationId)
        
        if (state != null && status != null) {
            val response = HealthResponse(
                name = status.name,
                applicationId = status.applicationId,
                status = status.state.name,
                healthy = status.state.name == "RUNNING",
                lastUpdated = status.lastUpdated,
                threadCount = status.metadata.size,
                details = mapOf(
                    "previousState" to (state.previousState?.name ?: ""),
                    "threads" to status.metadata.map { thread ->
                        mapOf(
                            "threadName" to thread.threadName,
                            "threadState" to thread.threadState,
                            "activeTasks" to thread.activeTasks,
                            "standbyTasks" to thread.standbyTasks
                        )
                    }
                )
            )
            return ResponseEntity.ok(response)
        } else {
            return ResponseEntity.status(404).body(
                HealthResponse(
                    name = "unknown",
                    applicationId = applicationId,
                    status = "NOT_FOUND",
                    healthy = false,
                    lastUpdated = null,
                    threadCount = 0
                )
            )
        }
    }
    
    @GetMapping("/health/summary")
    fun getHealthSummary(): ResponseEntity<Map<String, Any>> {
        logger.debug { "Getting health summary for all topologies" }
        
        val allStates = healthManager.getAllTopologyStates()
        val healthyTopologies = healthManager.getHealthyTopologies()
        val unhealthyTopologies = healthManager.getUnhealthyTopologies()
        
        val summary = mapOf(
            "total" to allStates.size,
            "healthy" to healthyTopologies.size,
            "unhealthy" to unhealthyTopologies.size,
            "healthyTopologies" to healthyTopologies,
            "unhealthyTopologies" to unhealthyTopologies,
            "overallHealth" to (unhealthyTopologies.isEmpty() && allStates.isNotEmpty())
        )
        
        return ResponseEntity.ok(summary)
    }
}

