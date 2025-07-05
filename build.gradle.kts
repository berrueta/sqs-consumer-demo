plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs:3.1.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:localstack:1.19.7")
    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("software.amazon.awssdk:sqs:2.25.62")
    testImplementation("org.awaitility:awaitility:4.2.1")
}

tasks.test {
    useJUnitPlatform()
}