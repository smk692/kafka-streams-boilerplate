package com.son.boilerplate.app.serdes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer


class JsonSerde<T>(private val clazz: Class<T>) : Serde<T> {
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    override fun serializer(): Serializer<T> = Serializer { _, data ->
        objectMapper.writeValueAsBytes(data)
    }

    override fun deserializer(): Deserializer<T> = Deserializer { _, bytes ->
        objectMapper.readValue(bytes, clazz)
    }
}