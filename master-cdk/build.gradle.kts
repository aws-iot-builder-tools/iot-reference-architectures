plugins {
    kotlin("jvm") version "1.4.21-2"
    id("application")
    id("java")
    id("idea")
    id("java-library")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val gradleDependencyVersion = "6.7.1"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

// Use the MasterApp main class when the JAR is invoked directly
application.mainClass.set("com.awssamples.MasterApp")

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }

// Specify all of our dependency versions
val awsCdkVersion = "1.85.0"
val vavrVersion = "0.10.3"
val slf4jVersion = "2.0.0-alpha1"
val jcabiVersion = "0.19.0"
val commonsLangVersion = "3.11"
val commonsIoVersion = "2.8.0"
val ztZipVersion = "1.14"
val resultsIteratorForAwsJavaSdkVersion = "11.0.7"
val daggerVersion = "2.30.1"
val junitVersion = "4.13.1"
val awsLambdaServletVersion = "0.0.30"

repositories {
    mavenCentral()
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
    maven(url = "https://jitpack.io")
}

dependencies {
    // Dagger code generation
    annotationProcessor("com.google.dagger:dagger-compiler:$daggerVersion")

    // Dependency injection with Dagger
    implementation("com.google.dagger:dagger:$daggerVersion")

    implementation("org.slf4j:slf4j-log4j12:$slf4jVersion")
    implementation("com.jcabi:jcabi-log:$jcabiVersion")

    implementation("software.amazon.awscdk:core:$awsCdkVersion")
    implementation("software.amazon.awscdk:iam:$awsCdkVersion")
    implementation("software.amazon.awscdk:sqs:$awsCdkVersion")
    implementation("software.amazon.awscdk:iot:$awsCdkVersion")
    implementation("software.amazon.awscdk:lambda:$awsCdkVersion")
    implementation("software.amazon.awscdk:dynamodb:$awsCdkVersion")
    implementation("software.amazon.awscdk:apigateway:$awsCdkVersion")
    implementation("io.vavr:vavr:$vavrVersion")
    implementation("org.gradle:gradle-tooling-api:$gradleDependencyVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.zeroturnaround:zt-zip:$ztZipVersion")
    implementation("com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion")
    implementation("com.github.aws-samples:aws-lambda-servlet:$awsLambdaServletVersion")

    testImplementation("junit:junit:$junitVersion")
}
