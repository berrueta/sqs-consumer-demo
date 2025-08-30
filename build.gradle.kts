plugins {
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs:3.3.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:localstack:1.21.3")
    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("org.awaitility:awaitility:4.2.1")
}

tasks.test {
    useJUnitPlatform()
}