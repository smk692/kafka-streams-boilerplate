package com.son.boilerplate.app.processor.test


import com.son.boilerplate.app.model.ARecord
import com.son.boilerplate.app.model.BRecord
import com.son.boilerplate.app.model.OutputRecord
import com.son.boilerplate.core.enum.KafkaTopics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class StreamsTopology {

    private val logger = LoggerFactory.getLogger(StreamsTopology::class.java)

    @Bean
    fun processStreams(streamsBuilder: StreamsBuilder): KStream<String, OutputRecord> {
        // 입력 스트림을 생성: Kafka 토픽 A에서 ARecord 타입의 데이터를 읽어옵니다.
        val aStream: KStream<String, ARecord> =
            streamsBuilder.stream(KafkaTopics.TOPIC_A.topic, Consumed.with(Serdes.String(), JsonSerde(ARecord::class.java)))

        // 상태 업데이트를 위한 KTable 생성: Kafka 토픽 B에서 BRecord 타입의 데이터를 읽어옵니다.
        val bTable: KTable<String, BRecord> =
            streamsBuilder.table(KafkaTopics.TOPIC_B.topic, Consumed.with(Serdes.String(), JsonSerde(BRecord::class.java)))

        logger.debug("AStream and BTable created")

        // AStream을 사용하여 BTable을 업데이트합니다.
        aStream
            .mapValues { aRecord -> BRecord(aRecord.orderId, aRecord.paymentStatus) }
            .to(KafkaTopics.TOPIC_B.topic, Produced.with(Serdes.String(), JsonSerde(BRecord::class.java)))

        logger.debug("BTable updated with AStream data")

        // BTable을 스트림으로 변환하여 처리합니다.
        val bStream: KStream<String, BRecord> = bTable.toStream()

        // BStream을 사용하여 최종 OutputRecord를 생성합니다.
        val outputStream: KStream<String, OutputRecord> = bStream.groupByKey(Grouped.with(Serdes.String(), JsonSerde(BRecord::class.java)))
            .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMinutes(5), Duration.ofMinutes(1)))
            .aggregate(
                { OutputRecord() },
                { key, bRecord, aggregate ->
                    aggregate.apply {
                        paymentComplete += if (bRecord.status == "결제완료") 1L else 0L
                        preparingForDelivery += if (bRecord.status == "배송준비중") 1L else 0L
                        inDelivery += if (bRecord.status == "배송중") 1L else 0L
                        delayInDelivery += if (bRecord.status == "지연") 1L else 0L
                    }
                },
                Materialized.with(Serdes.String(), JsonSerde(OutputRecord::class.java))
            )
            .toStream()
            .map { key, value -> KeyValue(key.key(), value) }

        outputStream.to(KafkaTopics.TOPIC_OUTPUT.topic, Produced.with(Serdes.String(), JsonSerde(OutputRecord::class.java)))

        logger.debug("Final output sent to topic-output")

        return outputStream
    }
}