package com.example.kafka.streams.app.infrastructure.kafka

import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class JsonSerde<T>(private val clazz: Class<T>) : Serde<T> {
    
    override fun serializer(): Serializer<T> = JsonSerializer()
    override fun deserializer(): Deserializer<T> = JsonDeserializer(clazz)
    
    companion object {
        inline fun <reified T> create(): JsonSerde<T> = JsonSerde(T::class.java)
    }
}

class JsonSerializer<T> : Serializer<T> {
    override fun serialize(topic: String?, data: T?): ByteArray? {
        return data?.let { ObjectMapperConfig.INSTANCE.writeValueAsBytes(it) }
    }
}

class JsonDeserializer<T>(private val clazz: Class<T>) : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        return data?.let { ObjectMapperConfig.INSTANCE.readValue(it, clazz) }
    }
}