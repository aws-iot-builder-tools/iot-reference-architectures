plugins {
    id("java")
    id("application")
    id("idea")

    // Creates fat JAR
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val gradleDependencyVersion = "7.0.2"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

application.mainClass.set("com.awslabs.aws.iot.resultsiterator.Example")

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }
tasks.shadowDistZip { enabled = false }
tasks.shadowDistTar { enabled = false }

// Specify all of our dependency versions
val awsSdk2Version = "2.16.66"

group = "com.awslabs.aws.iot.resultsiterator.jitpack.Example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("software.amazon.awssdk:iot:$awsSdk2Version")
}
