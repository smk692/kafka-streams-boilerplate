package com.example.kafka.streams.order.domain.events

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

data class ProcessedOrderEvent(
    @JsonProperty("orderId")
    val orderId: String,
    @JsonProperty("status")
    val status: String,
    @JsonProperty("processedAt")
    val processedAt: Instant,
    @JsonProperty("originalAmount")
    val originalAmount: BigDecimal? = null
)