plugins {
    kotlin("jvm") version "1.4.31"
    id("application")
    id("java")
    id("idea")
    id("java-library")
    id("com.github.ben-manes.versions") version "0.38.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val gradleDependencyVersion = "6.8.3"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

// Use the MasterApp main class when the JAR is invoked directly
application.mainClass.set("com.awssamples.MasterApp")

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }

// Specify all of our dependency versions
val awsCdkVersion = "1.93.0"
val vavrVersion = "0.10.3"
val slf4jVersion = "2.0.0-alpha1"
val jcabiVersion = "0.19.0"
val commonsLangVersion = "3.12.0"
val commonsIoVersion = "2.8.0"
val ztZipVersion = "1.14"
val resultsIteratorForAwsJavaSdkVersion = "11.0.8"
val daggerVersion = "2.33"
val junitVersion = "4.13.2"
val awsLambdaServletVersion = "0.2.4"
val awsCdkConstructsForJavaVersion = "0.5.6"

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
    maven(url = "https://jitpack.io")
}

dependencies {
    // Dagger code generation
    annotationProcessor("com.google.dagger:dagger-compiler:$daggerVersion")

    // Dependency injection with Dagger
    api("com.google.dagger:dagger:$daggerVersion")

    implementation("org.slf4j:slf4j-log4j12:$slf4jVersion")
    implementation("com.jcabi:jcabi-log:$jcabiVersion")

    api("software.amazon.awscdk:core:$awsCdkVersion")
    api("software.amazon.awscdk:iam:$awsCdkVersion")
    api("software.amazon.awscdk:sqs:$awsCdkVersion")
    api("software.amazon.awscdk:iot:$awsCdkVersion")
    api("software.amazon.awscdk:lambda:$awsCdkVersion")
    api("software.amazon.awscdk:dynamodb:$awsCdkVersion")
    api("software.amazon.awscdk:apigateway:$awsCdkVersion")
    implementation("io.vavr:vavr:$vavrVersion")
    api("org.gradle:gradle-tooling-api:$gradleDependencyVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    api("org.zeroturnaround:zt-zip:$ztZipVersion")
    api("com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion")

//    api("local:aws-lambda-servlet:1.0-SNAPSHOT")
    api("com.github.aws-samples:aws-lambda-servlet:$awsLambdaServletVersion")

    api("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJavaVersion")
//    api("local:aws-cdk-constructs-for-java:1.0-SNAPSHOT")

    testImplementation("junit:junit:$junitVersion")
}
