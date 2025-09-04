package com.example.kafka.streams.order.domain.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class ProcessedOrderEventTest {
    
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }
    
    @Test
    @DisplayName("ProcessedOrderEvent 생성 테스트")
    fun `should create ProcessedOrderEvent correctly`() {
        // given
        val orderId = "ORDER-101"
        val status = "HIGH_VALUE_PROCESSED"
        val processedAt = Instant.now()
        val originalAmount = BigDecimal("1500.00")
        
        // when
        val event = ProcessedOrderEvent(
            orderId = orderId,
            status = status,
            processedAt = processedAt,
            originalAmount = originalAmount
        )
        
        // then
        assertEquals(orderId, event.orderId)
        assertEquals(status, event.status)
        assertEquals(processedAt, event.processedAt)
        assertEquals(originalAmount, event.originalAmount)
    }
    
    @Test
    @DisplayName("ProcessedOrderEvent null originalAmount 허용 테스트")
    fun `should allow null originalAmount in ProcessedOrderEvent`() {
        // given
        val orderId = "ORDER-102"
        val status = "PROCESSED"
        val processedAt = Instant.now()
        
        // when
        val event = ProcessedOrderEvent(
            orderId = orderId,
            status = status,
            processedAt = processedAt,
            originalAmount = null
        )
        
        // then
        assertEquals(orderId, event.orderId)
        assertEquals(status, event.status)
        assertEquals(processedAt, event.processedAt)
        assertNull(event.originalAmount)
    }
    
    @Test
    @DisplayName("ProcessedOrderEvent JSON 직렬화 테스트")
    fun `should serialize ProcessedOrderEvent to JSON`() {
        // given
        val processedAt = Instant.parse("2024-01-01T10:00:00Z")
        val event = ProcessedOrderEvent(
            orderId = "ORDER-103",
            status = "STANDARD_PROCESSED",
            processedAt = processedAt,
            originalAmount = BigDecimal("250.75")
        )
        
        // when
        val json = objectMapper.writeValueAsString(event)
        val jsonNode = objectMapper.readTree(json)
        
        // then
        assertEquals("ORDER-103", jsonNode.get("orderId").asText())
        assertEquals("STANDARD_PROCESSED", jsonNode.get("status").asText())
        assertNotNull(jsonNode.get("processedAt"))
        assertEquals(BigDecimal("250.75"), jsonNode.get("originalAmount").decimalValue())
    }
    
    @Test
    @DisplayName("ProcessedOrderEvent JSON 역직렬화 테스트")
    fun `should deserialize JSON to ProcessedOrderEvent`() {
        // given
        val json = """
            {
                "orderId": "ORDER-104",
                "status": "SMALL_ORDER_PROCESSED",
                "processedAt": "2024-01-01T12:30:00Z",
                "originalAmount": 75.50
            }
        """.trimIndent()
        
        // when
        val event = objectMapper.readValue(json, ProcessedOrderEvent::class.java)
        
        // then
        assertEquals("ORDER-104", event.orderId)
        assertEquals("SMALL_ORDER_PROCESSED", event.status)
        assertEquals(Instant.parse("2024-01-01T12:30:00Z"), event.processedAt)
        assertEquals(BigDecimal("75.50"), event.originalAmount)
    }
    
    @Test
    @DisplayName("null originalAmount JSON 역직렬화 테스트")
    fun `should deserialize JSON with null originalAmount`() {
        // given
        val json = """
            {
                "orderId": "ORDER-105",
                "status": "CANCELLED",
                "processedAt": "2024-01-01T14:00:00Z",
                "originalAmount": null
            }
        """.trimIndent()
        
        // when
        val event = objectMapper.readValue(json, ProcessedOrderEvent::class.java)
        
        // then
        assertEquals("ORDER-105", event.orderId)
        assertEquals("CANCELLED", event.status)
        assertNotNull(event.processedAt)
        assertNull(event.originalAmount)
    }
    
    @Test
    @DisplayName("originalAmount 누락시 null로 처리 테스트")
    fun `should handle missing originalAmount as null`() {
        // given - originalAmount 필드가 없는 JSON
        val json = """
            {
                "orderId": "ORDER-106",
                "status": "PENDING",
                "processedAt": "2024-01-01T15:00:00Z"
            }
        """.trimIndent()
        
        // when
        val event = objectMapper.readValue(json, ProcessedOrderEvent::class.java)
        
        // then
        assertEquals("ORDER-106", event.orderId)
        assertEquals("PENDING", event.status)
        assertNotNull(event.processedAt)
        assertNull(event.originalAmount)
    }
    
    @Test
    @DisplayName("다양한 상태값 처리 테스트")
    fun `should handle various status values`() {
        // given
        val statuses = listOf(
            "HIGH_VALUE_PROCESSED",
            "STANDARD_PROCESSED",
            "SMALL_ORDER_PROCESSED",
            "FAILED",
            "CANCELLED",
            "PENDING"
        )
        
        // when & then
        statuses.forEach { status ->
            val event = ProcessedOrderEvent(
                orderId = "ORDER-TEST",
                status = status,
                processedAt = Instant.now(),
                originalAmount = BigDecimal("100.00")
            )
            assertEquals(status, event.status)
        }
    }
    
    @Test
    @DisplayName("ProcessedOrderEvent 동등성 테스트")
    fun `should correctly implement equals and hashCode`() {
        // given
        val processedAt = Instant.now()
        val event1 = ProcessedOrderEvent(
            orderId = "ORDER-107",
            status = "PROCESSED",
            processedAt = processedAt,
            originalAmount = BigDecimal("500.00")
        )
        val event2 = ProcessedOrderEvent(
            orderId = "ORDER-107",
            status = "PROCESSED",
            processedAt = processedAt,
            originalAmount = BigDecimal("500.00")
        )
        val event3 = ProcessedOrderEvent(
            orderId = "ORDER-108",
            status = "PROCESSED",
            processedAt = processedAt,
            originalAmount = BigDecimal("500.00")
        )
        
        // then
        assertEquals(event1, event2)
        assertNotEquals(event1, event3)
        assertEquals(event1.hashCode(), event2.hashCode())
    }
}