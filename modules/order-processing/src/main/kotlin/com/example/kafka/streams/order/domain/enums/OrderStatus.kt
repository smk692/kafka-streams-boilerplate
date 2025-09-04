package com.example.kafka.streams.order.domain.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class OrderStatus(@JsonValue val value: String) {
    HIGH_VALUE_PROCESSED("HIGH_VALUE_PROCESSED"),
    STANDARD_PROCESSED("STANDARD_PROCESSED"),
    SMALL_ORDER_PROCESSED("SMALL_ORDER_PROCESSED");
    
    companion object {
        fun fromString(value: String): OrderStatus {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown order status: $value")
        }
    }
}