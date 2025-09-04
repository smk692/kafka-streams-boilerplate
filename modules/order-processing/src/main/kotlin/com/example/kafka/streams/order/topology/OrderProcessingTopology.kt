package com.example.kafka.streams.order.topology
import com.example.kafka.streams.order.domain.enums.OrderStatus
import com.example.kafka.streams.order.domain.events.OrderEvent
import com.example.kafka.streams.order.domain.events.ProcessedOrderEvent
import com.example.kafka.streams.order.infrastructure.kafka.JsonSerde
import mu.KotlinLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import java.math.BigDecimal
import java.time.Instant

class OrderProcessingTopology {
    
    companion object {
        // 토픽명 상수 - 실제 토픽명 포함
        const val ORDERS_TOPIC = "orders"
        const val PROCESSED_ORDERS_TOPIC = "processed-orders"
        
        // 비즈니스 상수
        private val HIGH_VALUE_THRESHOLD = BigDecimal("1000.00")
        private val STANDARD_VALUE_THRESHOLD = BigDecimal("100.00")
    }
    
    private val logger = KotlinLogging.logger {}
    
    fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        
        logger.info { "Building Order Processing Topology" }
        
        val orders = builder.stream(
            ORDERS_TOPIC,
            Consumed.with(Serdes.String(), JsonSerde.create<OrderEvent>())
        )
        
        val processedOrders = orders
            .filter { _, order -> 
                val isValid = order.amount > BigDecimal.ZERO
                logger.debug { "Filtering order ${order.orderId}: valid=$isValid" }
                isValid
            }
            .peek { key, value -> 
                logger.info { "Processing order: key=$key, orderId=${value.orderId}, amount=${value.amount}" }
            }
            .mapValues { _, order -> 
                processOrder(order)
            }
        
        processedOrders.to(
            PROCESSED_ORDERS_TOPIC,
            Produced.with(Serdes.String(), JsonSerde.create<ProcessedOrderEvent>())
        )
        
        return builder.build().also {
            logger.info { "Order Processing Topology built successfully" }
        }
    }
    
    private fun processOrder(order: OrderEvent): ProcessedOrderEvent {
        // 비즈니스 로직: 주문 처리
        logger.debug { "Processing order logic for ${order.orderId}" }
        
        return ProcessedOrderEvent(
            orderId = order.orderId,
            status = when {
                order.amount >= HIGH_VALUE_THRESHOLD -> OrderStatus.HIGH_VALUE_PROCESSED.value
                order.amount >= STANDARD_VALUE_THRESHOLD -> OrderStatus.STANDARD_PROCESSED.value
                else -> OrderStatus.SMALL_ORDER_PROCESSED.value
            },
            processedAt = Instant.now(),
            originalAmount = order.amount
        )
    }
}