plugins {
    id("java")
    id("application")
    id("idea")

    // Creates fat JAR
    id("com.github.johnrengelman.shadow") version "7.1.2"

    // Adds dependencyUpdates task
    id("com.github.ben-manes.versions") version "0.42.0"
}

val gradleDependencyVersion = "7.2"

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
val resultsIteratorForAwsJavaSdkVersion = "29.0.25"
val junitVersion = "4.13.2"
val awsSdk2Version = "2.20.112"
val vavrVersion = "0.10.4"
val jcabiVersion = "0.22.0"

group = "com.awslabs.aws.iot.resultsiterator.jitpack.Example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
    maven(url = "https://plugins.gradle.org/m2/")
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion")
    implementation("software.amazon.awssdk:iot:$awsSdk2Version")
    implementation("io.vavr:vavr:$vavrVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("com.jcabi:jcabi-log:$jcabiVersion")
}
