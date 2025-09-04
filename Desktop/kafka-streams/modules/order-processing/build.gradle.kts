dependencies {
    // Spring Boot 기본 (웹 기능 제외)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    
    // Kafka Streams
    implementation("org.apache.kafka:kafka-streams")
    implementation("org.springframework.cloud:spring-cloud-stream")
    
    // JSON serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // Test Dependencies
    testImplementation("org.apache.kafka:kafka-streams-test-utils")
}

// jar 파일 생성 (실행 가능한 jar 아님)
tasks.jar {
    enabled = true
    archiveClassifier = ""
}

// bootJar는 비활성화 (라이브러리 모듈이므로)
tasks.bootJar {
    enabled = false
}