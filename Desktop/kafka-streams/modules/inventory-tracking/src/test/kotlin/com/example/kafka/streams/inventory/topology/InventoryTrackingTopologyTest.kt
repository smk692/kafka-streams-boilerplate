package com.example.kafka.streams.inventory.topology

import com.example.kafka.streams.inventory.domain.events.InventoryAlertEvent
import com.example.kafka.streams.inventory.domain.events.InventoryUpdateEvent
import com.example.kafka.streams.inventory.infrastructure.kafka.JsonSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.apache.kafka.streams.state.KeyValueStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class InventoryTrackingTopologyTest {
    
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, InventoryUpdateEvent>
    private lateinit var outputTopic: TestOutputTopic<String, InventoryAlertEvent>
    private lateinit var inventoryStore: KeyValueStore<String, Int>
    
    @BeforeEach
    fun setup() {
        val topology = InventoryTrackingTopology().buildTopology()
        val props = Properties().apply {
            put(StreamsConfig.APPLICATION_ID_CONFIG, "test-inventory-tracking")
            put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String()::class.java)
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String()::class.java)
        }
        
        testDriver = TopologyTestDriver(topology, props)
        
        inputTopic = testDriver.createInputTopic(
            InventoryTrackingTopology.INVENTORY_UPDATES_TOPIC,
            Serdes.String().serializer(),
            JsonSerde.create<InventoryUpdateEvent>().serializer()
        )
        
        outputTopic = testDriver.createOutputTopic(
            InventoryTrackingTopology.INVENTORY_ALERTS_TOPIC,
            Serdes.String().deserializer(),
            JsonSerde.create<InventoryAlertEvent>().deserializer()
        )
        
        inventoryStore = testDriver.getKeyValueStore(InventoryTrackingTopology.INVENTORY_STORE)
    }
    
    @AfterEach
    fun tearDown() {
        testDriver.close()
    }
    
    @Test
    @DisplayName("ADD 연산 재고 업데이트 테스트")
    fun `should handle ADD operation correctly`() {
        // given
        val updateEvent = InventoryUpdateEvent(
            productId = "PROD-001",
            warehouseId = "WH-001",
            quantity = 50,
            operation = "ADD",
            timestamp = System.currentTimeMillis()
        )
        
        // when
        inputTopic.pipeInput("PROD-001", updateEvent)
        
        // then
        val currentQuantity = inventoryStore.get("PROD-001")
        assertEquals(50, currentQuantity)
        assertTrue(outputTopic.isEmpty, "재고가 충분하므로 알림이 없어야 함")
    }
    
    @Test
    @DisplayName("REMOVE 연산 재고 업데이트 테스트")
    fun `should handle REMOVE operation correctly`() {
        // given - 먼저 재고를 추가
        val addEvent = InventoryUpdateEvent(
            productId = "PROD-002",
            warehouseId = "WH-002",
            quantity = 30,
            operation = "ADD",
            timestamp = System.currentTimeMillis()
        )
        inputTopic.pipeInput("PROD-002", addEvent)
        
        val removeEvent = InventoryUpdateEvent(
            productId = "PROD-002",
            warehouseId = "WH-002",
            quantity = 15,
            operation = "REMOVE",
            timestamp = System.currentTimeMillis()
        )
        
        // when
        inputTopic.pipeInput("PROD-002", removeEvent)
        
        // then
        val currentQuantity = inventoryStore.get("PROD-002")
        assertEquals(15, currentQuantity)
        assertTrue(outputTopic.isEmpty, "재고가 아직 충분하므로 알림이 없어야 함")
    }
    
    @Test
    @DisplayName("SET 연산 재고 설정 테스트")
    fun `should handle SET operation correctly`() {
        // given
        val setEvent = InventoryUpdateEvent(
            productId = "PROD-003",
            warehouseId = "WH-003",
            quantity = 25,
            operation = "SET",
            timestamp = System.currentTimeMillis()
        )
        
        // when
        inputTopic.pipeInput("PROD-003", setEvent)
        
        // then
        val currentQuantity = inventoryStore.get("PROD-003")
        assertEquals(25, currentQuantity)
    }
    
    @Test
    @DisplayName("LOW_STOCK 알림 생성 테스트")
    fun `should generate LOW_STOCK alert when quantity is below threshold`() {
        // given
        val updateEvent = InventoryUpdateEvent(
            productId = "PROD-004",
            warehouseId = "WH-004",
            quantity = 5, // LOW_STOCK_THRESHOLD(10) 미만
            operation = "SET",
            timestamp = System.currentTimeMillis()
        )
        
        // when
        inputTopic.pipeInput("PROD-004", updateEvent)
        
        // then
        val alert = outputTopic.readKeyValue()
        assertNotNull(alert)
        assertEquals("PROD-004", alert.key)
        assertEquals("PROD-004", alert.value.productId)
        assertEquals(5, alert.value.currentQuantity)
        assertEquals("LOW_STOCK", alert.value.alertType)
        assertNotNull(alert.value.alertedAt)
    }
    
    @Test
    @DisplayName("OUT_OF_STOCK 알림 생성 테스트")
    fun `should generate OUT_OF_STOCK alert when quantity is zero`() {
        // given - 먼저 재고 추가 후 모두 제거
        val addEvent = InventoryUpdateEvent(
            productId = "PROD-005",
            warehouseId = "WH-005",
            quantity = 10,
            operation = "ADD",
            timestamp = System.currentTimeMillis()
        )
        inputTopic.pipeInput("PROD-005", addEvent)
        
        val removeEvent = InventoryUpdateEvent(
            productId = "PROD-005",
            warehouseId = "WH-005",
            quantity = 10,
            operation = "REMOVE",
            timestamp = System.currentTimeMillis()
        )
        
        // when
        inputTopic.pipeInput("PROD-005", removeEvent)
        
        // then
        val alert = outputTopic.readKeyValue()
        assertNotNull(alert)
        assertEquals("PROD-005", alert.key)
        assertEquals("PROD-005", alert.value.productId)
        assertEquals(0, alert.value.currentQuantity)
        assertEquals("OUT_OF_STOCK", alert.value.alertType)
    }
    
    @Test
    @DisplayName("재고가 충분할 때 알림이 생성되지 않는 테스트")
    fun `should not generate alert when quantity is sufficient`() {
        // given
        val updateEvent = InventoryUpdateEvent(
            productId = "PROD-006",
            warehouseId = "WH-006",
            quantity = 20, // LOW_STOCK_THRESHOLD(10) 이상
            operation = "SET",
            timestamp = System.currentTimeMillis()
        )
        
        // when
        inputTopic.pipeInput("PROD-006", updateEvent)
        
        // then
        assertTrue(outputTopic.isEmpty, "재고가 충분하므로 알림이 없어야 함")
        assertEquals(20, inventoryStore.get("PROD-006"))
    }
    
    @Test
    @DisplayName("음수 수량 REMOVE 시 재고가 0 이하로 내려가지 않는 테스트")
    fun `should not allow negative inventory when removing more than available`() {
        // given - 먼저 재고 5개 추가
        val addEvent = InventoryUpdateEvent(
            productId = "PROD-007",
            warehouseId = "WH-007",
            quantity = 5,
            operation = "ADD",
            timestamp = System.currentTimeMillis()
        )
        inputTopic.pipeInput("PROD-007", addEvent)
        
        // 10개 제거 시도 (5개보다 많음)
        val removeEvent = InventoryUpdateEvent(
            productId = "PROD-007",
            warehouseId = "WH-007",
            quantity = 10,
            operation = "REMOVE",
            timestamp = System.currentTimeMillis()
        )
        
        // when
        inputTopic.pipeInput("PROD-007", removeEvent)
        
        // then
        val currentQuantity = inventoryStore.get("PROD-007")
        assertEquals(0, currentQuantity, "재고는 0 미만으로 내려가지 않아야 함")
        
        // 두 개의 알림이 생성되어야 함: 처음 5개 추가시 LOW_STOCK, 10개 제거 후 OUT_OF_STOCK
        val alerts = outputTopic.readKeyValuesToList()
        assertEquals(2, alerts.size, "LOW_STOCK과 OUT_OF_STOCK 알림 2개가 생성되어야 함")
        
        // 첫 번째 알림은 LOW_STOCK (5개)
        assertEquals("LOW_STOCK", alerts[0].value.alertType)
        assertEquals(5, alerts[0].value.currentQuantity)
        
        // 두 번째 알림은 OUT_OF_STOCK (0개)
        assertEquals("OUT_OF_STOCK", alerts[1].value.alertType)
        assertEquals(0, alerts[1].value.currentQuantity)
    }
    
    @Test
    @DisplayName("다중 업데이트 연산 테스트")
    fun `should handle multiple operations correctly`() {
        // given
        val productId = "PROD-008"
        val updates = listOf(
            InventoryUpdateEvent(productId, "WH-008", 100, "ADD", System.currentTimeMillis()),     // 0 -> 100
            InventoryUpdateEvent(productId, "WH-008", 50, "REMOVE", System.currentTimeMillis()),   // 100 -> 50
            InventoryUpdateEvent(productId, "WH-008", 20, "ADD", System.currentTimeMillis()),      // 50 -> 70
            InventoryUpdateEvent(productId, "WH-008", 65, "REMOVE", System.currentTimeMillis())    // 70 -> 5 (LOW_STOCK 알림)
        )
        
        // when
        updates.forEach { update ->
            inputTopic.pipeInput(productId, update)
        }
        
        // then
        val finalQuantity = inventoryStore.get(productId)
        assertEquals(5, finalQuantity)
        
        // LOW_STOCK 알림이 생성되어야 함
        val alert = outputTopic.readKeyValue()
        assertNotNull(alert)
        assertEquals("LOW_STOCK", alert.value.alertType)
        assertEquals(5, alert.value.currentQuantity)
    }
    
    @Test
    @DisplayName("경계값 테스트 - 정확히 임계값")
    fun `should handle boundary values correctly`() {
        // given - 정확히 LOW_STOCK_THRESHOLD(10) 설정
        val updateEvent = InventoryUpdateEvent(
            productId = "PROD-009",
            warehouseId = "WH-009",
            quantity = 10,
            operation = "SET",
            timestamp = System.currentTimeMillis()
        )
        
        // when
        inputTopic.pipeInput("PROD-009", updateEvent)
        
        // then
        assertEquals(10, inventoryStore.get("PROD-009"))
        assertTrue(outputTopic.isEmpty, "재고가 정확히 임계값이므로 알림이 없어야 함")
        
        // 이제 1개 제거하여 임계값 미만으로 만들기
        val removeEvent = InventoryUpdateEvent(
            productId = "PROD-009",
            warehouseId = "WH-009",
            quantity = 1,
            operation = "REMOVE",
            timestamp = System.currentTimeMillis()
        )
        inputTopic.pipeInput("PROD-009", removeEvent)
        
        // LOW_STOCK 알림이 생성되어야 함
        val alert = outputTopic.readKeyValue()
        assertNotNull(alert)
        assertEquals("LOW_STOCK", alert.value.alertType)
        assertEquals(9, alert.value.currentQuantity)
    }
    
    @Test
    @DisplayName("다중 제품 재고 추적 테스트")
    fun `should track multiple products independently`() {
        // given
        val updates = listOf(
            InventoryUpdateEvent("PROD-A", "WH-A", 50, "ADD", System.currentTimeMillis()),
            InventoryUpdateEvent("PROD-B", "WH-B", 5, "SET", System.currentTimeMillis()),  // LOW_STOCK
            InventoryUpdateEvent("PROD-C", "WH-C", 0, "SET", System.currentTimeMillis())   // OUT_OF_STOCK
        )
        
        // when
        updates.forEach { update ->
            inputTopic.pipeInput(update.productId, update)
        }
        
        // then
        assertEquals(50, inventoryStore.get("PROD-A"))
        assertEquals(5, inventoryStore.get("PROD-B"))
        assertEquals(0, inventoryStore.get("PROD-C"))
        
        // 2개의 알림이 생성되어야 함 (PROD-B: LOW_STOCK, PROD-C: OUT_OF_STOCK)
        val alerts = outputTopic.readKeyValuesToList()
        assertEquals(2, alerts.size)
        
        val prodBAlert = alerts.find { it.key == "PROD-B" }
        val prodCAlert = alerts.find { it.key == "PROD-C" }
        
        assertNotNull(prodBAlert)
        assertEquals("LOW_STOCK", prodBAlert!!.value.alertType)
        
        assertNotNull(prodCAlert)
        assertEquals("OUT_OF_STOCK", prodCAlert!!.value.alertType)
    }
}