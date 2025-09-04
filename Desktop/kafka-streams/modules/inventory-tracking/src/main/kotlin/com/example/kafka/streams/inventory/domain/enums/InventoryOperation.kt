package com.example.kafka.streams.inventory.domain.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class InventoryOperation(@JsonValue val value: String) {
    ADD("ADD"),
    REMOVE("REMOVE"),
    SET("SET");
    
    companion object {
        fun fromString(value: String): InventoryOperation {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown inventory operation: $value")
        }
    }
}