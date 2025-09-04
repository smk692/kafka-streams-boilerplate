import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10" apply false
    kotlin("plugin.spring") version "1.9.10" apply false
    kotlin("plugin.jpa") version "1.9.10" apply false
    id("org.springframework.boot") version "3.1.5" apply false
    id("io.spring.dependency-management") version "1.1.3" apply false
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
    }
    
    val springCloudVersion = "2022.0.4"
    
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
            mavenBom("org.testcontainers:testcontainers-bom:1.19.1")
        }
    }
    
    dependencies {
        // Kotlin 기본
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        
        // 로깅
        "implementation"("io.github.microutils:kotlin-logging:3.0.5")
        
        // 테스트 공통
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("io.mockk:mockk:1.13.8")
        "testImplementation"("com.ninja-squad:springmockk:4.0.2")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test")
    }
    
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "17"
        }
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}