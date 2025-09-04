package com.example.kafka.streams.inventory.topology
import com.example.kafka.streams.inventory.domain.enums.InventoryOperation
import com.example.kafka.streams.inventory.domain.events.InventoryAlertEvent
import com.example.kafka.streams.inventory.domain.events.InventoryUpdateEvent
import com.example.kafka.streams.inventory.infrastructure.kafka.JsonSerde
import mu.KotlinLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant

class InventoryTrackingTopology {
    
    companion object {
        // 토픽명 상수 - 실제 토픽명 포함
        const val INVENTORY_UPDATES_TOPIC = "inventory-updates"
        const val INVENTORY_ALERTS_TOPIC = "inventory-alerts"
        
        // State Store 상수 - 명확한 스토어명  
        const val INVENTORY_STORE = "inventory-store"
        
        // 비즈니스 상수
        private const val LOW_STOCK_THRESHOLD = 10
        
        // State Store Materialized 설정 - 타입 안전하고 재사용 가능
        private val INVENTORY_MATERIALIZED = Materialized.`as`<String, Int, KeyValueStore<Bytes, ByteArray>>(INVENTORY_STORE)
            .withKeySerde(Serdes.String())
            .withValueSerde(Serdes.Integer())
    }
    
    private val logger = KotlinLogging.logger {}
    
    fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        
        logger.info { "Building Inventory Tracking Topology" }
        
        val inventoryUpdates = builder.stream(
            INVENTORY_UPDATES_TOPIC,
            Consumed.with(Serdes.String(), JsonSerde.create<InventoryUpdateEvent>())
        )

        // 재고 업데이트를 집계하여 현재 재고량 추적
        val currentInventory = inventoryUpdates
            .groupByKey()
            .aggregate(
                { 0 }, // 초기값
                { key, update, currentQuantity ->
                    val operation = InventoryOperation.fromString(update.operation)
                    val newQuantity = when (operation) {
                        InventoryOperation.ADD -> currentQuantity + update.quantity
                        InventoryOperation.REMOVE -> (currentQuantity - update.quantity).coerceAtLeast(0)
                        InventoryOperation.SET -> update.quantity
                    }
                    logger.debug { "Updated inventory for $key: $currentQuantity -> $newQuantity" }
                    newQuantity
                },
                INVENTORY_MATERIALIZED
            )

        // 재고 알림 생성 (재고가 10 미만이면 LOW_STOCK, 0이면 OUT_OF_STOCK)
        val inventoryAlerts = currentInventory
            .toStream()
            .filter { productId, quantity ->
                val shouldAlert = quantity < LOW_STOCK_THRESHOLD
                if (shouldAlert) {
                    logger.info { "Generating alert for product $productId: quantity=$quantity" }
                }
                shouldAlert
            }
            .mapValues { productId, quantity ->
                InventoryAlertEvent(
                    productId = productId,
                    currentQuantity = quantity,
                    alertType = if (quantity == 0) "OUT_OF_STOCK" else "LOW_STOCK",
                    alertedAt = Instant.now()
                )
            }

        inventoryAlerts.to(
            INVENTORY_ALERTS_TOPIC,
            Produced.with(Serdes.String(), JsonSerde.create<InventoryAlertEvent>())
        )
        
        return builder.build().also {
            logger.info { "Inventory Tracking Topology built successfully" }
        }
    }
}