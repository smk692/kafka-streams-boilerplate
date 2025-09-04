package com.example.kafka.streams.order.infrastructure.kafka

import com.example.kafka.streams.order.domain.events.OrderEvent
import com.example.kafka.streams.order.domain.events.ProcessedOrderEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant

class JsonSerdeTest {
    
    @Test
    @DisplayName("OrderEvent Serde 직렬화/역직렬화 테스트")
    fun `should serialize and deserialize OrderEvent correctly`() {
        // given
        val serde = JsonSerde.create<OrderEvent>()
        val orderEvent = OrderEvent(
            orderId = "ORDER-201",
            customerId = "CUST-201",
            productId = "PROD-201",
            quantity = 5,
            price = BigDecimal(25.50),
            timestamp = 1234567890L,
            status = "NEW"
        )
        val topic = "test-topic"
        
        // when
        val serializer = serde.serializer()
        val deserializer = serde.deserializer()
        
        val bytes = serializer.serialize(topic, orderEvent)
        val deserialized = deserializer.deserialize(topic, bytes)
        
        // then
        assertNotNull(bytes)
        assertEquals(orderEvent.orderId, deserialized.orderId)
        assertEquals(orderEvent.customerId, deserialized.customerId)
        assertEquals(orderEvent.productId, deserialized.productId)
        assertEquals(orderEvent.quantity, deserialized.quantity)
        assertEquals(orderEvent.price, deserialized.price)
        assertEquals(orderEvent.timestamp, deserialized.timestamp)
        assertEquals(orderEvent.status, deserialized.status)
        
        // cleanup
        serializer.close()
        deserializer.close()
        serde.close()
    }
    
    @Test
    @DisplayName("ProcessedOrderEvent Serde 직렬화/역직렬화 테스트")
    fun `should serialize and deserialize ProcessedOrderEvent correctly`() {
        // given
        val serde = JsonSerde.create<ProcessedOrderEvent>()
        val processedEvent = ProcessedOrderEvent(
            orderId = "ORDER-202",
            status = "HIGH_VALUE_PROCESSED",
            processedAt = Instant.parse("2024-01-01T10:00:00Z"),
            originalAmount = BigDecimal("1500.00")
        )
        val topic = "test-topic"
        
        // when
        val serializer = serde.serializer()
        val deserializer = serde.deserializer()
        
        val bytes = serializer.serialize(topic, processedEvent)
        val deserialized = deserializer.deserialize(topic, bytes)
        
        // then
        assertNotNull(bytes)
        assertEquals(processedEvent.orderId, deserialized.orderId)
        assertEquals(processedEvent.status, deserialized.status)
        assertEquals(processedEvent.processedAt, deserialized.processedAt)
        assertEquals(processedEvent.originalAmount, deserialized.originalAmount)
        
        // cleanup
        serializer.close()
        deserializer.close()
        serde.close()
    }
    
    @Test
    @DisplayName("null 값 직렬화 테스트")
    fun `should handle null value serialization`() {
        // given
        val serde = JsonSerde.create<OrderEvent>()
        val serializer = serde.serializer()
        val topic = "test-topic"
        
        // when
        val bytes = serializer.serialize(topic, null)
        
        // then
        assertNull(bytes)
        
        // cleanup
        serializer.close()
        serde.close()
    }
    
    @Test
    @DisplayName("null 바이트 역직렬화 테스트")
    fun `should handle null bytes deserialization`() {
        // given
        val serde = JsonSerde.create<OrderEvent>()
        val deserializer = serde.deserializer()
        val topic = "test-topic"
        
        // when
        val result = deserializer.deserialize(topic, null)
        
        // then
        assertNull(result)
        
        // cleanup
        deserializer.close()
        serde.close()
    }
    
    @Test
    @DisplayName("빈 바이트 배열 역직렬화 테스트")
    fun `should handle empty byte array deserialization`() {
        // given
        val serde = JsonSerde.create<OrderEvent>()
        val deserializer = serde.deserializer()
        val topic = "test-topic"
        val emptyBytes = ByteArray(0)
        
        // when
        val result = deserializer.deserialize(topic, emptyBytes)
        
        // then
        assertNull(result)
        
        // cleanup
        deserializer.close()
        serde.close()
    }
    
    @Test
    @DisplayName("잘못된 JSON 역직렬화 오류 테스트")
    fun `should throw exception for invalid JSON`() {
        // given
        val serde = JsonSerde.create<OrderEvent>()
        val deserializer = serde.deserializer()
        val topic = "test-topic"
        val invalidJson = "{ invalid json }".toByteArray()
        
        // when & then
        assertThrows<Exception> {
            deserializer.deserialize(topic, invalidJson)
        }
        
        // cleanup
        deserializer.close()
        serde.close()
    }
    
    @Test
    @DisplayName("ProcessedOrderEvent null originalAmount 처리 테스트")
    fun `should handle ProcessedOrderEvent with null originalAmount`() {
        // given
        val serde = JsonSerde.create<ProcessedOrderEvent>()
        val processedEvent = ProcessedOrderEvent(
            orderId = "ORDER-203",
            status = "CANCELLED",
            processedAt = Instant.now(),
            originalAmount = null
        )
        val topic = "test-topic"
        
        // when
        val serializer = serde.serializer()
        val deserializer = serde.deserializer()
        
        val bytes = serializer.serialize(topic, processedEvent)
        val deserialized = deserializer.deserialize(topic, bytes)
        
        // then
        assertNotNull(bytes)
        assertEquals(processedEvent.orderId, deserialized.orderId)
        assertEquals(processedEvent.status, deserialized.status)
        assertNotNull(deserialized.processedAt)
        assertNull(deserialized.originalAmount)
        
        // cleanup
        serializer.close()
        deserializer.close()
        serde.close()
    }
    
    @Test
    @DisplayName("대용량 OrderEvent 직렬화 테스트")
    fun `should handle large OrderEvent`() {
        // given
        val serde = JsonSerde.create<OrderEvent>()
        val largeOrderId = "ORDER-" + "X".repeat(1000)
        val orderEvent = OrderEvent(
            orderId = largeOrderId,
            customerId = "CUST-204",
            productId = "PROD-204",
            quantity = 999999,
            price = BigDecimal("999999.99"),
            timestamp = System.currentTimeMillis(),
            status = "NEW"
        )
        val topic = "test-topic"
        
        // when
        val serializer = serde.serializer()
        val deserializer = serde.deserializer()
        
        val bytes = serializer.serialize(topic, orderEvent)
        val deserialized = deserializer.deserialize(topic, bytes)
        
        // then
        assertNotNull(bytes)
        assertEquals(orderEvent.orderId, deserialized.orderId)
        assertEquals(orderEvent.quantity, deserialized.quantity)
        assertEquals(orderEvent.price, deserialized.price)
        
        // cleanup
        serializer.close()
        deserializer.close()
        serde.close()
    }
}