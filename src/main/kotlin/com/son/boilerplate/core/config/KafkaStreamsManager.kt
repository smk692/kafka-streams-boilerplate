package com.son.boilerplate.core.config


import com.son.boilerplate.core.annotation.KafkaStreamsTopology
import com.son.boilerplate.core.annotation.TopologyBuilder
import jakarta.annotation.PostConstruct
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Component
import java.util.*

@Component
class KafkaStreamsManager(
    private val applicationContext: ApplicationContext
) {
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.streams.state-dir}")
    private lateinit var stateDir: String

    private val logger = LoggerFactory.getLogger(KafkaStreamsManager::class.java)
    private val kafkaStreamsMap: MutableMap<String, KafkaStreams> = mutableMapOf()

    /**
     * 애플리케이션 시작 시 모든 토폴로지를 초기화하고 시작합니다.
     */
    @PostConstruct
    fun initializeTopologies() {
        val topologyBuilders = applicationContext.getBeansOfType(TopologyBuilder::class.java).values

        if (topologyBuilders.isEmpty()) {
            logger.warn("No TopologyBuilder beans found.")
            return
        }

        topologyBuilders.forEach { builder ->
            val name = builder.topologyName
            val streams = createKafkaStreams(builder, name)
            kafkaStreamsMap[name] = streams

            try {
                streams.start()
                logger.info("Started Kafka Streams for topology '$name'.")
            } catch (e: Exception) {
                logger.error("Error starting Kafka Streams for topology '$name': ${e.message}", e)
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            shutdownAllTopologies()
        })
    }


    /**
     * TopologyBuilder를 기반으로 KafkaStreams 인스턴스를 생성합니다.
     *
     * @param builder TopologyBuilder 인스턴스
     * @param applicationId Kafka Streams 애플리케이션 ID
     * @return 생성된 KafkaStreams 인스턴스
     */
    private fun createKafkaStreams(builder: TopologyBuilder, applicationId: String): KafkaStreams {
        val streamsBuilder = StreamsBuilder().apply {
            builder.buildTopology(this)
        }

        // Kafka Streams 설정을 Properties로 정의합니다.
        val properties = Properties().apply {
            put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
            put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(StreamsConfig.STATE_DIR_CONFIG, stateDir)
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().javaClass.name)
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().javaClass.name)
        }

        // 정의된 Properties를 사용하여 StreamsConfig 객체를 생성합니다.
        return KafkaStreams(streamsBuilder.build(), StreamsConfig(properties))
    }

    /**
     * 특정 토폴로지를 시작하거나 재시작합니다.
     *
     * @param name 시작 또는 재시작할 토폴로지의 이름
     * @return 작업 결과 메시지
     */
    fun startOrRestartTopology(name: String): String {
        val streams = kafkaStreamsMap[name]
        return when {
            streams == null -> "토폴로지 '$name'이 존재하지 않습니다."
            streams.state().isRunningOrRebalancing -> "토폴로지 '$name'은 이미 실행 중이거나 리밸런싱 중입니다."
            else -> {
                streams.resume()
                "토폴로지 '$name' 재시작되었습니다."
            }
        }
    }

    /**
     * 특정 토폴로지를 중지합니다.
     *
     * @param name 중지할 토폴로지의 이름
     */
    fun stopTopology(name: String) {
        kafkaStreamsMap[name]?.takeIf { it.state().isRunningOrRebalancing }?.close()
    }

    /**
     * 현재 등록된 모든 토폴로지의 상태를 조회합니다.
     *
     * @return 토폴로지 이름과 상태의 리스트
     */
    fun getTopologyList(): List<Pair<String, String>> {
        return kafkaStreamsMap.map { (name, streams) ->
            name to streams.state().toString()
        }
    }

    /**
     * 모든 KTable의 상태를 조회합니다.
     *
     * @return 모든 KTable의 상태를 담은 맵. 키는 storeName, 값은 그 스토어에 저장된 키-값 쌍.
     */
    fun getAllKTableStates(): Map<String, Map<String, String>> {
        val allStates = mutableMapOf<String, Map<String, String>>()
        kafkaStreamsMap.values.forEach { streams ->
            streams.allMetadata().flatMap { it.stateStoreNames() }.toSet().forEach { storeName ->
                val state = retrieveStoreState(streams, storeName)
                if (state.isNotEmpty()) {
                    allStates[storeName] = state
                }
            }
        }
        return allStates
    }

    /**
     * 특정 KTable의 상태를 조회합니다.
     *
     * @param storeName 조회할 KTable의 스토어 이름
     * @return KTable의 키-값 쌍을 담은 맵
     */
    fun getKTableState(storeName: String): Map<String, Map<String, String>> {
        return kafkaStreamsMap.values
            .flatMap { streams ->
                val state = retrieveStoreState(streams, storeName)
                if (state.isNotEmpty()) {
                    listOf(storeName to state)
                } else {
                    emptyList()
                }
            }
            .toMap()
    }

    /**
     * 상태 저장소의 상태를 조회합니다.
     *
     * @param streams KafkaStreams 인스턴스
     * @param storeName 조회할 상태 저장소 이름
     * @return 상태 저장소의 상태를 담은 맵
     */
    private fun retrieveStoreState(streams: KafkaStreams, storeName: String): Map<String, String> {
        return try {
            val storeQueryParameters = StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.keyValueStore<String, String>())
            val store: ReadOnlyKeyValueStore<String, String> = streams.store(storeQueryParameters)
            store.all().asSequence().map { it.key to it.value }.toMap()
        } catch (e: Exception) {
            logger.error("오류: 저장소 '$storeName' 상태 조회 중 오류 발생: ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * 애플리케이션 종료 시 모든 KafkaStreams 인스턴스를 안전하게 종료합니다.
     */
    fun shutdownAllTopologies() {
        kafkaStreamsMap.values.forEach { kafkaStreams ->
            try {
                kafkaStreams.close()
            } catch (e: Exception) {
                logger.error("Kafka Streams 종료 중 오류 발생: ${e.message}", e)
            }
        }
    }
}