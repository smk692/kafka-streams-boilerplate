package com.example.kafka.streams.inventory.domain.events

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class InventoryUpdateEvent(
    @JsonProperty("productId")
    val productId: String,
    @JsonProperty("warehouseId")
    val warehouseId: String,
    @JsonProperty("quantity")
    val quantity: Int,
    @JsonProperty("operation")
    val operation: String, // ADD, REMOVE, SET
    @JsonProperty("timestamp")
    val timestamp: Long
)

data class InventoryAlertEvent(
    @JsonProperty("productId")
    val productId: String,
    @JsonProperty("currentQuantity")
    val currentQuantity: Int,
    @JsonProperty("alertType")
    val alertType: String, // LOW_STOCK, OUT_OF_STOCK
    @JsonProperty("alertedAt")
    val alertedAt: Instant
)