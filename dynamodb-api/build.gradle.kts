plugins {
    kotlin("jvm") version "1.5.30"
    id("application")
    id("java")
    id("idea")
    id("java-library")

    // Creates fat JAR
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

val gradleDependencyVersion = "7.2"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

// Required for shadow JAR but not used
application.mainClass.set("not-applicable")

repositories {
    mavenCentral()
    google()

    maven(url = "https://plugins.gradle.org/m2/")
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
    maven(url = "https://jitpack.io")
}

// Specify all of our dependency versions
val slf4jVersion = "1.7.32"
val gsonVersion = "2.8.8"
val awsSdk2Version = "2.17.33"
val junitVersion = "4.13.2"
val awsLambdaJavaCoreVersion = "1.2.1"
val vavrVersion = "0.10.4"
val vavrGsonVersion = "0.10.2"
val commonsCodecVersion = "1.15"
val resultsIteratorForAwsJavaSdkVersion = "29.0.18"
val xrayVersion = "2.7.1"
val jacksonVersion = "2.12.5"
val awsCdkConstructsForJavaVersion = "0.16.11"

group = "com.awssamples.iot.dynamodb.Api"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("com.google.code.gson:gson:$gsonVersion")

    implementation("com.amazonaws:aws-lambda-java-core:$awsLambdaJavaCoreVersion")

    implementation("software.amazon.awssdk:sqs:$awsSdk2Version")
    implementation("software.amazon.awssdk:dynamodb:$awsSdk2Version")
    implementation("software.amazon.awssdk:iotdataplane:$awsSdk2Version")
    implementation("software.amazon.awssdk:iot:$awsSdk2Version")

    // For converting payloads to hex strings
    implementation("commons-codec:commons-codec:$commonsCodecVersion")

    implementation("org.slf4j:slf4j-log4j12:$slf4jVersion")

    implementation("io.vavr:vavr:$vavrVersion")
    implementation("io.vavr:vavr-gson:$vavrGsonVersion")

    implementation("com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion")
    implementation("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJavaVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    testImplementation("junit:junit:$junitVersion")
}

task("synth", JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    main = "com.awssamples.dynamodbapi.SqsToIotCoreStack"
}
