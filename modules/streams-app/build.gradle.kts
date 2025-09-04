plugins {
    application
}

dependencies {
    // Spring Boot & Web
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    
    // Spring Cloud Stream & Kafka
    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka-streams")
    implementation("org.apache.kafka:kafka-streams")
    
    // Metrics & Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-core")
    
    // Domain modules
    implementation(project(":modules:order-processing"))
    implementation(project(":modules:inventory-tracking"))
    
    // Test Dependencies
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-support")
    testImplementation("org.apache.kafka:kafka-streams-test-utils")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:junit-jupiter")
}

// Main class for bootRun task
springBoot {
    mainClass.set("com.example.kafka.streams.app.KafkaStreamsApplicationKt")
}

