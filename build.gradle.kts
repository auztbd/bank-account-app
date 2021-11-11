import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.spring") version "1.5.31"
    id("org.springframework.boot") version "2.5.6"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
    withType<Test> {
        useJUnitPlatform()
        testLogging.showStackTraces = true
    }
}

val springBootVersion = "2.5.6"
val exposedVersion = "0.36.1"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-amqp:$springBootVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("io.github.microutils:kotlin-logging:2.0.11")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")

    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jodatime:$exposedVersion")
    implementation("com.h2database:h2:1.4.200")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("io.rest-assured:rest-assured:4.4.0")
    testImplementation("io.rest-assured:json-path:4.4.0")
    testImplementation("com.ninja-squad:springmockk:3.0.1")
}
