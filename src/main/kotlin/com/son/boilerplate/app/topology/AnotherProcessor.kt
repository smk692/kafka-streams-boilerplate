package com.son.boilerplate.app.topology

import com.son.boilerplate.core.annotation.KafkaStreamsTopology
import com.son.boilerplate.core.annotation.TopologyBuilder
import com.son.boilerplate.core.enum.AnotherTopologyTopics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

@KafkaStreamsTopology(name = "anotherTopology")
class AnotherProcessor : TopologyBuilder {
    override fun buildTopology(builder: StreamsBuilder) {
        val source: KStream<String, String> = builder.stream(AnotherTopologyTopics.INPUT_1.topic)
        source.to(AnotherTopologyTopics.OUTPUT.topic, Produced.with(Serdes.String(), Serdes.String()))
    }
}
