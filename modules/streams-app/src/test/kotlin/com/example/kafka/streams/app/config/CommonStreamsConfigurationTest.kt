package com.example.kafka.streams.app.config

import org.apache.kafka.streams.StreamsConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

@SpringBootTest(classes = [CommonStreamsConfiguration::class])
@TestPropertySource(properties = [
    "kafka.bootstrap-servers=localhost:9092"
])
class CommonStreamsConfigurationTest {
    
    @Autowired
    private lateinit var commonStreamsConfiguration: CommonStreamsConfiguration
    
    @Test
    @DisplayName("CommonStreamsConfiguration Bean 생성 테스트")
    fun `should create CommonStreamsConfiguration bean`() {
        assertNotNull(commonStreamsConfiguration)
    }
    
    @Test
    @DisplayName("공통 Streams Properties 생성 테스트")
    fun `should create common streams properties with correct values`() {
        // when
        val properties = commonStreamsConfiguration.createCommonStreamsProperties("localhost:9092")
        
        // then
        assertNotNull(properties)
        assertEquals("localhost:9092", properties.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
        assertEquals(
            org.apache.kafka.common.serialization.Serdes.String()::class.java,
            properties[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG]
        )
        assertEquals(
            org.apache.kafka.common.serialization.Serdes.String()::class.java, 
            properties[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG]
        )
        assertEquals(
            StreamsConfig.EXACTLY_ONCE_V2,
            properties[StreamsConfig.PROCESSING_GUARANTEE_CONFIG]
        )
        assertEquals(1000, properties[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG])
        assertEquals(
            org.apache.kafka.streams.errors.LogAndContinueExceptionHandler::class.java,
            properties[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG]
        )
    }
    
    @Test
    @DisplayName("다양한 Bootstrap Servers 설정 테스트")
    fun `should handle various bootstrap servers configurations`() {
        // given
        val testCases = listOf(
            "localhost:9092",
            "kafka1:9092,kafka2:9092,kafka3:9092",
            "broker1.example.com:9092",
            "127.0.0.1:9092"
        )
        
        // when & then
        testCases.forEach { bootstrapServers ->
            val properties = commonStreamsConfiguration.createCommonStreamsProperties(bootstrapServers)
            assertEquals(bootstrapServers, properties.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
        }
    }
    
    @Test
    @DisplayName("Properties 타입 검증 테스트")
    fun `should return Properties instance`() {
        // when
        val properties = commonStreamsConfiguration.createCommonStreamsProperties("localhost:9092")
        
        // then
        assertTrue(properties is Properties)
        assertTrue(properties.size > 0)
    }
    
    @Test
    @DisplayName("모든 필수 설정 값 존재 확인 테스트")
    fun `should contain all required configuration properties`() {
        // when
        val properties = commonStreamsConfiguration.createCommonStreamsProperties("localhost:9092")
        
        // then
        val requiredProperties = listOf(
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
            StreamsConfig.PROCESSING_GUARANTEE_CONFIG,
            StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,
            StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG
        )
        
        requiredProperties.forEach { property ->
            assertTrue(properties.containsKey(property), "Property $property should be present")
            assertNotNull(properties[property], "Property $property should have a value")
        }
    }
    
    @Test
    @DisplayName("설정값 타입 검증 테스트")
    fun `should have correct property value types`() {
        // when
        val properties = commonStreamsConfiguration.createCommonStreamsProperties("localhost:9092")
        
        // then
        // String 값들
        assertTrue(properties.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG) is String)
        assertTrue(properties[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] is Class<*>)
        assertTrue(properties[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] is Class<*>)
        assertTrue(properties[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] is String)
        
        // Integer 값 확인
        val commitInterval = properties[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG]
        assertTrue(commitInterval is Int)
        assertEquals(1000, commitInterval as Int)
    }
    
    @Test
    @DisplayName("EXACTLY_ONCE_V2 보장 설정 검증")
    fun `should configure exactly once v2 processing guarantee`() {
        // when
        val properties = commonStreamsConfiguration.createCommonStreamsProperties("localhost:9092")
        
        // then
        assertEquals(
            StreamsConfig.EXACTLY_ONCE_V2,
            properties[StreamsConfig.PROCESSING_GUARANTEE_CONFIG]
        )
    }
    
    @Test
    @DisplayName("역직렬화 예외 핸들러 설정 검증")
    fun `should configure deserialization exception handler`() {
        // when
        val properties = commonStreamsConfiguration.createCommonStreamsProperties("localhost:9092")
        
        // then
        assertEquals(
            org.apache.kafka.streams.errors.LogAndContinueExceptionHandler::class.java,
            properties[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG]
        )
    }
    
    @Test
    @DisplayName("빈 Bootstrap Servers 처리 테스트")
    fun `should handle empty bootstrap servers`() {
        // when
        val properties = commonStreamsConfiguration.createCommonStreamsProperties("")
        
        // then
        assertEquals("", properties.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
        // 다른 설정들은 여전히 존재해야 함
        assertNotNull(properties[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG])
    }
    
    @Test
    @DisplayName("Properties 불변성 테스트")
    fun `should return independent properties instances`() {
        // when
        val properties1 = commonStreamsConfiguration.createCommonStreamsProperties("server1:9092")
        val properties2 = commonStreamsConfiguration.createCommonStreamsProperties("server2:9092")
        
        // then
        assertNotSame(properties1, properties2, "Should return different instances")
        assertNotEquals(
            properties1.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG),
            properties2.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)
        )
        
        // 다른 설정들은 동일해야 함
        assertEquals(
            properties1[StreamsConfig.PROCESSING_GUARANTEE_CONFIG],
            properties2[StreamsConfig.PROCESSING_GUARANTEE_CONFIG]
        )
    }
}