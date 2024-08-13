package com.son.boilerplate.core.annotation

import org.apache.kafka.streams.StreamsBuilder

interface TopologyBuilder {
    val topologyName: String  // 각 토폴로지의 이름을 제공

    fun buildTopology(builder: StreamsBuilder)
}