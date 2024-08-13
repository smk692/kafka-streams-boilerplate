package com.son.boilerplate.app.api

import com.son.boilerplate.core.config.KafkaStreamsManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StreamsService(
    private val kafkaStreamsManager: KafkaStreamsManager
) {

    private val logger = LoggerFactory.getLogger(StreamsService::class.java)

    /**
     * 특정 토폴로지를 시작하거나 재시작합니다.
     *
     * @param name 시작 또는 재시작할 토폴로지의 이름
     * @return 작업 결과 메시지
     */
    fun startOrRestartTopology(name: String): String {
        return kafkaStreamsManager.startOrRestartTopology(name)
    }

    /**
     * 특정 토폴로지를 중지합니다.
     *
     * @param name 중지할 토폴로지의 이름
     */
    fun stopTopology(name: String) {
        kafkaStreamsManager.stopTopology(name)
    }

    /**
     * 현재 등록된 모든 토폴로지의 상태를 조회합니다.
     *
     * @return 토폴로지 이름과 상태의 리스트
     */
    fun getTopologyList(): List<Pair<String, String>> {
        return kafkaStreamsManager.getTopologyList()
    }

    /**
     * 모든 KTable의 상태를 조회합니다.
     *
     * @return 모든 KTable의 상태를 담은 맵. 키는 storeName, 값은 그 스토어에 저장된 키-값 쌍.
     */
    fun getAllKTableStates(): Map<String, Map<String, String>> {
        return kafkaStreamsManager.getAllKTableStates()
    }

    /**
     * 특정 KTable의 상태를 조회합니다.
     *
     * @param storeName 조회할 KTable의 스토어 이름
     * @return KTable의 키-값 쌍을 담은 맵
     */
    fun getKTableState(storeName: String): Map<String, Map<String, String>> {
        return kafkaStreamsManager.getKTableState(storeName)
    }

    /**
     * 애플리케이션 종료 시 모든 KafkaStreams 인스턴스를 안전하게 종료합니다.
     */
    fun shutdownAllTopologies() {
        kafkaStreamsManager.shutdownAllTopologies()
    }
}