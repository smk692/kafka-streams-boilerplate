package com.son.boilerplate.app.api

import org.apache.kafka.streams.KafkaStreams
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/topologies")
class StreamsApi(private val kafkaStreamsService: StreamsService) {

    @PostMapping("/{name}/start-or-restart")
    fun startOrRestartTopology(@PathVariable name: String): String {
        return kafkaStreamsService.startOrRestartTopology(name)
    }

    @PostMapping("/{name}/stop")
    fun stopTopology(@PathVariable name: String) {
        kafkaStreamsService.stopTopology(name)
    }

    @GetMapping("/list")
    fun getTopologyList(): List<Pair<String, String>> {
        return kafkaStreamsService.getTopologyList()
    }


    /**
     * 모든 KTable의 상태를 조회하는 API.
     *
     * @return 모든 KTable의 상태를 담은 맵. 각 상태 저장소의 이름과 그에 해당하는 키-값 쌍의 맵을 반환.
     */
    @GetMapping("/state/all")
    fun getAllKTableStates(): Map<String, Map<String, String>> {
        return kafkaStreamsService.getAllKTableStates()
    }

    @GetMapping("/state/{storeName}")
    fun getKTableState(@PathVariable storeName: String): Map<String, Map<String, String>> {
        return kafkaStreamsService.getKTableState(storeName)
    }
}