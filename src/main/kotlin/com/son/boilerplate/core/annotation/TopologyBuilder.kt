package com.son.boilerplate.core.annotation

import org.apache.kafka.streams.StreamsBuilder

fun interface TopologyBuilder {
    fun buildTopology(builder: StreamsBuilder)
}