plugins {
    kotlin("jvm") version "2.2.0"
    id("application")
    id("java")
    id("idea")
    id("java-library")

    // Creates fat JAR
    id("com.github.johnrengelman.shadow") version "8.1.1"

    // Adds dependencyUpdates task
    id("com.github.ben-manes.versions") version "0.52.0"
}

val gradleDependencyVersion = "7.2"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}
application.mainClass.set("com.awslabs.aws.iot.websockets.Example")

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }
tasks.shadowDistZip { enabled = false }
tasks.shadowDistTar { enabled = false }

// Specify all of our dependency versions
val awsIotCoreWebsockets = "4.0.1"
val awsSdkV2Version = "2.31.72"
val junitVersion = "4.13.2"
val awaitilityVersion = "4.3.0"

// NOTE: Do not use 1.2.3 or you will get null pointer exceptions
val pahoVersion = "1.2.5"

group = "com.awslabs.aws.iot.websockets.jitpack.Example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
    maven(url = "https://plugins.gradle.org/m2/")
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.awslabs:aws-iot-core-websockets:$awsIotCoreWebsockets")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:$pahoVersion")

    testImplementation("software.amazon.awssdk:regions:$awsSdkV2Version")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
}
