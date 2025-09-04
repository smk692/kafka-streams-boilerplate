package com.example.kafka.streams.order.domain.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class OrderEventTest {
    
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }
    
    @Test
    @DisplayName("OrderEvent 생성 및 amount 계산 테스트")
    fun `should create OrderEvent and calculate amount correctly`() {
        // given
        val orderId = "ORDER-001"
        val customerId = "CUST-001"
        val productId = "PROD-001"
        val quantity = 5
        val price = BigDecimal("100.50")
        val timestamp = 1234567890L
        val status = "NEW"
        
        // when
        val orderEvent = OrderEvent(
            orderId = orderId,
            customerId = customerId,
            productId = productId,
            quantity = quantity,
            price = price,
            timestamp = timestamp,
            status = status
        )
        
        // then
        assertEquals(orderId, orderEvent.orderId)
        assertEquals(customerId, orderEvent.customerId)
        assertEquals(productId, orderEvent.productId)
        assertEquals(quantity, orderEvent.quantity)
        assertEquals(price, orderEvent.price)
        assertEquals(timestamp, orderEvent.timestamp)
        assertEquals(status, orderEvent.status)
        assertEquals(BigDecimal("502.50"), orderEvent.amount)
    }
    
    @Test
    @DisplayName("amount 계산 로직 검증 - 정수 계산")
    fun `should calculate amount correctly for integer values`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-002",
            customerId = "CUST-002",
            productId = "PROD-002",
            quantity = 10,
            price = BigDecimal("50"),
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        val amount = orderEvent.amount
        
        // then
        assertEquals(BigDecimal("500"), amount)
    }
    
    @Test
    @DisplayName("amount 계산 로직 검증 - 소수점 계산")
    fun `should calculate amount correctly for decimal values`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-003",
            customerId = "CUST-003",
            productId = "PROD-003",
            quantity = 3,
            price = BigDecimal("99.99"),
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        val amount = orderEvent.amount
        
        // then
        assertEquals(BigDecimal("299.97"), amount)
    }
    
    @Test
    @DisplayName("JSON 직렬화 테스트")
    fun `should serialize OrderEvent to JSON correctly`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-004",
            customerId = "CUST-004",
            productId = "PROD-004",
            quantity = 2,
            price = BigDecimal(25.50),
            timestamp = 1234567890L,
            status = "PENDING"
        )
        
        // when
        val json = objectMapper.writeValueAsString(orderEvent)
        val jsonNode = objectMapper.readTree(json)
        
        // then
        assertEquals("ORDER-004", jsonNode.get("orderId").asText())
        assertEquals("CUST-004", jsonNode.get("customerId").asText())
        assertEquals("PROD-004", jsonNode.get("productId").asText())
        assertEquals(2, jsonNode.get("quantity").asInt())
        assertEquals(BigDecimal(25.50), jsonNode.get("price").decimalValue())
        assertEquals(1234567890L, jsonNode.get("timestamp").asLong())
        assertEquals("PENDING", jsonNode.get("status").asText())
    }
    
    @Test
    @DisplayName("JSON 역직렬화 테스트")
    fun `should deserialize JSON to OrderEvent correctly`() {
        // given
        val json = """
            {
                "orderId": "ORDER-005",
                "customerId": "CUST-005",
                "productId": "PROD-005",
                "quantity": 7,
                "price": 15.75,
                "timestamp": 1234567890,
                "status": "CONFIRMED"
            }
        """.trimIndent()
        
        // when
        val orderEvent = objectMapper.readValue(json, OrderEvent::class.java)
        
        // then
        assertEquals("ORDER-005", orderEvent.orderId)
        assertEquals("CUST-005", orderEvent.customerId)
        assertEquals("PROD-005", orderEvent.productId)
        assertEquals(7, orderEvent.quantity)
        assertEquals(BigDecimal("15.75"), orderEvent.price)
        assertEquals(1234567890L, orderEvent.timestamp)
        assertEquals("CONFIRMED", orderEvent.status)
        assertEquals(BigDecimal("110.25"), orderEvent.amount)
    }
    
    @Test
    @DisplayName("필수 필드 누락시 역직렬화 오류 테스트")
    fun `should fail deserialization when required field is missing`() {
        // given - orderId 누락된 JSON
        val json = """
            {
                "customerId": "CUST-006",
                "productId": "PROD-006",
                "quantity": 1,
                "price": 10.00,
                "timestamp": 1234567890,
                "status": "NEW"
            }
        """.trimIndent()
        
        // when & then
        assertThrows<Exception> {
            objectMapper.readValue(json, OrderEvent::class.java)
        }
    }
    
    @Test
    @DisplayName("0 수량 주문 amount 계산 테스트")
    fun `should calculate zero amount for zero quantity`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-007",
            customerId = "CUST-007",
            productId = "PROD-007",
            quantity = 0,
            price = BigDecimal(100.00),
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        val amount = orderEvent.amount
        
        // then
        assertEquals(BigDecimal.ZERO, amount)
    }
}