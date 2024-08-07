package com.son.boilerplate.app.model

data class OutputRecord(
    var paymentComplete: Long = 0,
    var preparingForDelivery: Long = 0,
    var inDelivery: Long = 0,
    var delayInDelivery: Long = 0
)
