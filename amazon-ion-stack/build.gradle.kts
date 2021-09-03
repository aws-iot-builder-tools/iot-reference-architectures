plugins {
    kotlin("jvm") version "1.5.30"
    id("application")
    id("java")
    id("idea")

    // Creates fat JAR
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

idea.module.isDownloadSources = true
idea.module.isDownloadJavadoc = true

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

val gradleDependencyVersion = "7.2"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

repositories {
    mavenCentral()
    google()
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
}

application.mainClassName = "not-applicable"

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }
tasks.shadowDistZip { enabled = false }
tasks.shadowDistTar { enabled = false }

tasks.shadowJar {
    // AWS Lambda specific - fixes "StatusLogger Unrecognized format specifier [d]" errors, from https://stackoverflow.com/questions/48033792/log4j2-error-statuslogger-unrecognized-conversion-specifier
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
}

// Specify all of our dependency versions
val slf4jVersion = "1.7.32"
val gsonVersion = "2.8.8"
val awsSdk2Version = "2.17.29"
val junitVersion = "4.13.2"
val awsLambdaJavaCoreVersion = "1.2.1"
val vavrVersion = "0.10.4"
val daggerVersion = "2.38.1"
val resultsIteratorForAwsJavaSdkVersion = "29.0.14"
val awsCdkVersion = "1.120.0"
val awsCdkConstructsForJavaVersion = "0.16.8"

dependencies {
    // Dagger code generation
    annotationProcessor("com.google.dagger:dagger-compiler:$daggerVersion")

    // Dependency injection with Dagger
    api("com.google.dagger:dagger:$daggerVersion")

    implementation("io.vavr:vavr:$vavrVersion")

    implementation("org.slf4j:slf4j-log4j12:$slf4jVersion")
    api("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJavaVersion")
    implementation("com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion")

    testImplementation("junit:junit:$junitVersion")
}

task("synth", JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    main = "com.awssamples.amazonion.AmazonIonStack"
}

