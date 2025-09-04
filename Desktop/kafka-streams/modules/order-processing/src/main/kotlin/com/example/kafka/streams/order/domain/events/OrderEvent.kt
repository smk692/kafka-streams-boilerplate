package com.example.kafka.streams.order.domain.events

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

data class OrderEvent(
    @JsonProperty("orderId")
    val orderId: String,
    @JsonProperty("customerId")
    val customerId: String,
    @JsonProperty("productId")
    val productId: String,
    @JsonProperty("quantity")
    val quantity: Int,
    @JsonProperty("price")
    val price: BigDecimal,
    @JsonProperty("timestamp")
    val timestamp: Long,
    @JsonProperty("status")
    val status: String
) {
    val amount: BigDecimal
        get() = price.multiply(BigDecimal(quantity))
}

