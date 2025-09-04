package com.example.kafka.streams.inventory.domain.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class InventoryEventTest {
    
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }
    
    @Test
    @DisplayName("InventoryUpdateEvent 생성 테스트")
    fun `should create InventoryUpdateEvent correctly`() {
        // given
        val productId = "PROD-001"
        val warehouseId = "WH-001"
        val quantity = 100
        val operation = "ADD"
        val timestamp = 1234567890L
        
        // when
        val event = InventoryUpdateEvent(
            productId = productId,
            warehouseId = warehouseId,
            quantity = quantity,
            operation = operation,
            timestamp = timestamp
        )
        
        // then
        assertEquals(productId, event.productId)
        assertEquals(warehouseId, event.warehouseId)
        assertEquals(quantity, event.quantity)
        assertEquals(operation, event.operation)
        assertEquals(timestamp, event.timestamp)
    }
    
    @Test
    @DisplayName("InventoryUpdateEvent JSON 직렬화 테스트")
    fun `should serialize InventoryUpdateEvent to JSON`() {
        // given
        val event = InventoryUpdateEvent(
            productId = "PROD-002",
            warehouseId = "WH-002",
            quantity = 50,
            operation = "REMOVE",
            timestamp = 1234567890L
        )
        
        // when
        val json = objectMapper.writeValueAsString(event)
        val jsonNode = objectMapper.readTree(json)
        
        // then
        assertEquals("PROD-002", jsonNode.get("productId").asText())
        assertEquals("WH-002", jsonNode.get("warehouseId").asText())
        assertEquals(50, jsonNode.get("quantity").asInt())
        assertEquals("REMOVE", jsonNode.get("operation").asText())
        assertEquals(1234567890L, jsonNode.get("timestamp").asLong())
    }
    
    @Test
    @DisplayName("InventoryUpdateEvent JSON 역직렬화 테스트")
    fun `should deserialize JSON to InventoryUpdateEvent`() {
        // given
        val json = """
            {
                "productId": "PROD-003",
                "warehouseId": "WH-003",
                "quantity": 75,
                "operation": "SET",
                "timestamp": 1234567890
            }
        """.trimIndent()
        
        // when
        val event = objectMapper.readValue(json, InventoryUpdateEvent::class.java)
        
        // then
        assertEquals("PROD-003", event.productId)
        assertEquals("WH-003", event.warehouseId)
        assertEquals(75, event.quantity)
        assertEquals("SET", event.operation)
        assertEquals(1234567890L, event.timestamp)
    }
    
    @Test
    @DisplayName("다양한 operation 값 테스트")
    fun `should handle various operation values`() {
        // given
        val operations = listOf("ADD", "REMOVE", "SET", "UPDATE", "DELETE")
        
        // when & then
        operations.forEach { operation ->
            val event = InventoryUpdateEvent(
                productId = "PROD-TEST",
                warehouseId = "WH-TEST",
                quantity = 10,
                operation = operation,
                timestamp = System.currentTimeMillis()
            )
            assertEquals(operation, event.operation)
        }
    }
    
    @Test
    @DisplayName("음수 재고량 허용 테스트")
    fun `should allow negative quantity values`() {
        // given
        val event = InventoryUpdateEvent(
            productId = "PROD-004",
            warehouseId = "WH-004",
            quantity = -25,
            operation = "REMOVE",
            timestamp = 1234567890L
        )
        
        // then
        assertEquals(-25, event.quantity)
    }
    
    @Test
    @DisplayName("0 재고량 허용 테스트")
    fun `should allow zero quantity values`() {
        // given
        val event = InventoryUpdateEvent(
            productId = "PROD-005",
            warehouseId = "WH-005",
            quantity = 0,
            operation = "SET",
            timestamp = 1234567890L
        )
        
        // then
        assertEquals(0, event.quantity)
    }
    
    @Test
    @DisplayName("필수 필드 누락 시 역직렬화 오류 테스트")
    fun `should fail deserialization when required field is missing`() {
        // given - productId 누락된 JSON
        val json = """
            {
                "warehouseId": "WH-006",
                "quantity": 30,
                "operation": "ADD",
                "timestamp": 1234567890
            }
        """.trimIndent()
        
        // when & then
        assertThrows<Exception> {
            objectMapper.readValue(json, InventoryUpdateEvent::class.java)
        }
    }
    
    @Test
    @DisplayName("InventoryAlertEvent 생성 테스트")
    fun `should create InventoryAlertEvent correctly`() {
        // given
        val productId = "PROD-007"
        val currentQuantity = 5
        val alertType = "LOW_STOCK"
        val alertedAt = Instant.now()
        
        // when
        val alert = InventoryAlertEvent(
            productId = productId,
            currentQuantity = currentQuantity,
            alertType = alertType,
            alertedAt = alertedAt
        )
        
        // then
        assertEquals(productId, alert.productId)
        assertEquals(currentQuantity, alert.currentQuantity)
        assertEquals(alertType, alert.alertType)
        assertEquals(alertedAt, alert.alertedAt)
    }
    
    @Test
    @DisplayName("InventoryAlertEvent JSON 직렬화/역직렬화 테스트")
    fun `should serialize and deserialize InventoryAlertEvent`() {
        // given
        val alertedAt = Instant.parse("2024-01-01T10:00:00Z")
        val alert = InventoryAlertEvent(
            productId = "PROD-008",
            currentQuantity = 0,
            alertType = "OUT_OF_STOCK",
            alertedAt = alertedAt
        )
        
        // when
        val json = objectMapper.writeValueAsString(alert)
        val deserialized = objectMapper.readValue(json, InventoryAlertEvent::class.java)
        
        // then
        assertEquals(alert.productId, deserialized.productId)
        assertEquals(alert.currentQuantity, deserialized.currentQuantity)
        assertEquals(alert.alertType, deserialized.alertType)
        assertEquals(alert.alertedAt, deserialized.alertedAt)
    }
    
    @Test
    @DisplayName("다양한 알림 타입 테스트")
    fun `should handle various alert types`() {
        // given
        val alertTypes = listOf("LOW_STOCK", "OUT_OF_STOCK", "RESTOCK_NEEDED", "OVERSTOCKED")
        
        // when & then
        alertTypes.forEach { alertType ->
            val alert = InventoryAlertEvent(
                productId = "PROD-TEST",
                currentQuantity = 10,
                alertType = alertType,
                alertedAt = Instant.now()
            )
            assertEquals(alertType, alert.alertType)
        }
    }
    
    @Test
    @DisplayName("InventoryUpdateEvent 동등성 테스트")
    fun `should correctly implement equals and hashCode for InventoryUpdateEvent`() {
        // given
        val event1 = InventoryUpdateEvent(
            productId = "PROD-009",
            warehouseId = "WH-009",
            quantity = 100,
            operation = "ADD",
            timestamp = 1234567890L
        )
        val event2 = InventoryUpdateEvent(
            productId = "PROD-009",
            warehouseId = "WH-009",
            quantity = 100,
            operation = "ADD",
            timestamp = 1234567890L
        )
        val event3 = InventoryUpdateEvent(
            productId = "PROD-010",
            warehouseId = "WH-009",
            quantity = 100,
            operation = "ADD",
            timestamp = 1234567890L
        )
        
        // then
        assertEquals(event1, event2)
        assertNotEquals(event1, event3)
        assertEquals(event1.hashCode(), event2.hashCode())
    }
}