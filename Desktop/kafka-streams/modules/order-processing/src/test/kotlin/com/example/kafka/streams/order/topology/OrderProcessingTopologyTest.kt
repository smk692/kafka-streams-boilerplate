package com.example.kafka.streams.order.topology

import com.example.kafka.streams.order.domain.events.OrderEvent
import com.example.kafka.streams.order.domain.events.ProcessedOrderEvent
import com.example.kafka.streams.order.infrastructure.kafka.JsonSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class OrderProcessingTopologyTest {
    
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, OrderEvent>
    private lateinit var outputTopic: TestOutputTopic<String, ProcessedOrderEvent>
    
    @BeforeEach
    fun setup() {
        val topology = OrderProcessingTopology().buildTopology()
        val props = Properties().apply {
            put(StreamsConfig.APPLICATION_ID_CONFIG, "test-order-processing")
            put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String()::class.java)
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String()::class.java)
        }
        
        testDriver = TopologyTestDriver(topology, props)
        
        inputTopic = testDriver.createInputTopic(
            OrderProcessingTopology.ORDERS_TOPIC,
            Serdes.String().serializer(),
            JsonSerde.create<OrderEvent>().serializer()
        )
        
        outputTopic = testDriver.createOutputTopic(
            OrderProcessingTopology.PROCESSED_ORDERS_TOPIC,
            Serdes.String().deserializer(),
            JsonSerde.create<ProcessedOrderEvent>().deserializer()
        )
    }
    
    @AfterEach
    fun tearDown() {
        testDriver.close()
    }
    
    @Test
    @DisplayName("고가주문 처리 테스트 - HIGH_VALUE_PROCESSED")
    fun `should process high value order correctly`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-1001",
            customerId = "CUST-001",
            productId = "PROD-001",
            quantity = 10,
            price = BigDecimal("150.00"), // amount = 1500.00 (>= 1000)
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        inputTopic.pipeInput("ORDER-1001", orderEvent)
        
        // then
        val output = outputTopic.readKeyValue()
        assertNotNull(output)
        assertEquals("ORDER-1001", output.key)
        assertEquals("ORDER-1001", output.value.orderId)
        assertEquals("HIGH_VALUE_PROCESSED", output.value.status)
        assertEquals(BigDecimal("1500.00"), output.value.originalAmount)
        assertNotNull(output.value.processedAt)
    }
    
    @Test
    @DisplayName("표준주문 처리 테스트 - STANDARD_PROCESSED")
    fun `should process standard order correctly`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-1002",
            customerId = "CUST-002",
            productId = "PROD-002",
            quantity = 5,
            price = BigDecimal("50.00"), // amount = 250.00 (>= 100, < 1000)
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        inputTopic.pipeInput("ORDER-1002", orderEvent)
        
        // then
        val output = outputTopic.readKeyValue()
        assertNotNull(output)
        assertEquals("ORDER-1002", output.key)
        assertEquals("ORDER-1002", output.value.orderId)
        assertEquals("STANDARD_PROCESSED", output.value.status)
        assertEquals(BigDecimal("250.00"), output.value.originalAmount)
    }
    
    @Test
    @DisplayName("소액주문 처리 테스트 - SMALL_ORDER_PROCESSED")
    fun `should process small order correctly`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-1003",
            customerId = "CUST-003",
            productId = "PROD-003",
            quantity = 2,
            price = BigDecimal("25.00"), // amount = 50.00 (< 100)
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        inputTopic.pipeInput("ORDER-1003", orderEvent)
        
        // then
        val output = outputTopic.readKeyValue()
        assertNotNull(output)
        assertEquals("ORDER-1003", output.key)
        assertEquals("ORDER-1003", output.value.orderId)
        assertEquals("SMALL_ORDER_PROCESSED", output.value.status)
        assertEquals(BigDecimal("50.00"), output.value.originalAmount)
    }
    
    @Test
    @DisplayName("경계값 테스트 - 정확히 1000.00")
    fun `should handle boundary value 1000 as high value`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-1004",
            customerId = "CUST-004",
            productId = "PROD-004",
            quantity = 10,
            price = BigDecimal("100.00"), // amount = 1000.00 (= 1000)
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        inputTopic.pipeInput("ORDER-1004", orderEvent)
        
        // then
        val output = outputTopic.readKeyValue()
        assertEquals("HIGH_VALUE_PROCESSED", output.value.status)
        assertEquals(BigDecimal("1000.00"), output.value.originalAmount)
    }
    
    @Test
    @DisplayName("경계값 테스트 - 정확히 100.00")
    fun `should handle boundary value 100 as standard`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-1005",
            customerId = "CUST-005",
            productId = "PROD-005",
            quantity = 1,
            price = BigDecimal("100.00"), // amount = 100.00 (= 100)
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        inputTopic.pipeInput("ORDER-1005", orderEvent)
        
        // then
        val output = outputTopic.readKeyValue()
        assertEquals("STANDARD_PROCESSED", output.value.status)
        assertEquals(BigDecimal("100.00"), output.value.originalAmount)
    }
    
    @Test
    @DisplayName("0 이하 금액 주문 필터링 테스트")
    fun `should filter out orders with zero or negative amount`() {
        // given
        val zeroAmountOrder = OrderEvent(
            orderId = "ORDER-1006",
            customerId = "CUST-006",
            productId = "PROD-006",
            quantity = 0,
            price = BigDecimal("100.00"), // amount = 0
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        val negativeAmountOrder = OrderEvent(
            orderId = "ORDER-1007",
            customerId = "CUST-007",
            productId = "PROD-007",
            quantity = 1,
            price = BigDecimal("-50.00"), // amount = -50
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        inputTopic.pipeInput("ORDER-1006", zeroAmountOrder)
        inputTopic.pipeInput("ORDER-1007", negativeAmountOrder)
        
        // then
        assertTrue(outputTopic.isEmpty, "출력 토픽에 메시지가 없어야 함")
    }
    
    @Test
    @DisplayName("다중 주문 처리 테스트")
    fun `should process multiple orders correctly`() {
        // given
        val orders = listOf(
            OrderEvent("ORDER-1008", "CUST-008", "PROD-008", 20, BigDecimal("60.00"), 1234567890L, "NEW"), // HIGH_VALUE: 1200
            OrderEvent("ORDER-1009", "CUST-009", "PROD-009", 3, BigDecimal("40.00"), 1234567890L, "NEW"),  // STANDARD: 120
            OrderEvent("ORDER-1010", "CUST-010", "PROD-010", 1, BigDecimal("50.00"), 1234567890L, "NEW")   // SMALL: 50
        )
        
        // when
        orders.forEach { order ->
            inputTopic.pipeInput(order.orderId, order)
        }
        
        // then
        val outputs = outputTopic.readKeyValuesToList()
        assertEquals(3, outputs.size)
        
        // 첫 번째 주문 검증
        assertEquals("HIGH_VALUE_PROCESSED", outputs[0].value.status)
        assertEquals(BigDecimal("1200.00"), outputs[0].value.originalAmount)
        
        // 두 번째 주문 검증
        assertEquals("STANDARD_PROCESSED", outputs[1].value.status)
        assertEquals(BigDecimal("120.00"), outputs[1].value.originalAmount)
        
        // 세 번째 주문 검증
        assertEquals("SMALL_ORDER_PROCESSED", outputs[2].value.status)
        assertEquals(BigDecimal("50.00"), outputs[2].value.originalAmount)
    }
    
    @Test
    @DisplayName("processedAt 필드 시간 검증")
    fun `should set processedAt timestamp correctly`() {
        // given
        val beforeProcessing = System.currentTimeMillis()
        val orderEvent = OrderEvent(
            orderId = "ORDER-1011",
            customerId = "CUST-011",
            productId = "PROD-011",
            quantity = 1,
            price = BigDecimal("200.00"),
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        inputTopic.pipeInput("ORDER-1011", orderEvent)
        val afterProcessing = System.currentTimeMillis()
        
        // then
        val output = outputTopic.readKeyValue()
        val processedTime = output.value.processedAt.toEpochMilli()
        assertTrue(processedTime >= beforeProcessing && processedTime <= afterProcessing,
            "processedAt 시간이 처리 시간 범위 내에 있어야 함")
    }
    
    @Test
    @DisplayName("토폴로지에 추가 메시지가 없는지 확인")
    fun `should have no more messages after processing`() {
        // given
        val orderEvent = OrderEvent(
            orderId = "ORDER-1012",
            customerId = "CUST-012",
            productId = "PROD-012",
            quantity = 1,
            price = BigDecimal("100.00"),
            timestamp = 1234567890L,
            status = "NEW"
        )
        
        // when
        inputTopic.pipeInput("ORDER-1012", orderEvent)
        outputTopic.readKeyValue() // 첫 번째 메시지 읽기
        
        // then
        assertTrue(outputTopic.isEmpty, "추가 메시지가 없어야 함")
    }
}