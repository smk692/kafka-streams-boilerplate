package com.son.boilerplate.app.topology

import com.son.boilerplate.core.annotation.KafkaStreamsTopology
import com.son.boilerplate.core.annotation.TopologyBuilder
import com.son.boilerplate.core.enum.StateStores
import com.son.boilerplate.core.enum.WordCountTopics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

@KafkaStreamsTopology(name = "wordCount")
class WordCountProcessor : TopologyBuilder {

    override fun buildTopology(builder: StreamsBuilder) {
        val source: KStream<String, String> = builder.stream(WordCountTopics.INPUT.topic)
        val counts: KTable<String, Long> = source
            .flatMapValues { value -> value.lowercase(Locale.getDefault()).split("\\W+".toRegex()) }
            .groupBy { _, value -> value }
            .count(
                Materialized.`as`<String, Long, KeyValueStore<Bytes, ByteArray>>(StateStores.WORD_COUNT_STORE.store)
                .withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Long())
            )
        counts.toStream().to(WordCountTopics.OUTPUT.topic, Produced.with(Serdes.String(), Serdes.Long()))
    }
}