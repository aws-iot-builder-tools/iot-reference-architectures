plugins {
    kotlin("jvm") version "1.5.21"
    id("application")
    id("java")
    id("idea")
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }
tasks.shadowDistZip { enabled = false }
tasks.shadowDistTar { enabled = false }

application.mainClassName = "io.vertx.fargate.applications.NonTlsMqtt"

tasks.shadowJar {
    // Create a shadow JAR with a specific name
    archiveName = "vertx.jar"

    // To prevent broken log4j2 configurations - see https://stackoverflow.com/a/61475766
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)

    isZip64 = true
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

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
    // Required for Gradle Tooling API
    maven(url = "https://repo.gradle.org/gradle/libs-releases/")
}

sourceSets.create("integrationTest") {
    java {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
        compileClasspath += sourceSets.test.get().output
        runtimeClasspath += sourceSets.test.get().output

        srcDir(file("src/integration-test/java"))
    }
}

configurations.getByName("integrationTestImplementation") { extendsFrom(configurations.testImplementation.get()) }
configurations.getByName("integrationTestApi") { extendsFrom(configurations.testApi.get()) }

val integrationTestTask = tasks.register("integrationTest", Test::class) {
    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = sourceSets.getByName("integrationTest").output.classesDirs
    classpath = sourceSets.getByName("integrationTest").runtimeClasspath
    outputs.upToDateWhen { false }
    mustRunAfter(tasks.getByName("test"))
}

// Specify all of our dependency versions
val jbbpVersion = "2.0.3"
val awsSdk2Version = "2.17.29"
val vertxVersion = "4.1.2"
val gsonVersion = "2.8.8"
val pahoMqttv3Version = "1.2.5"
val awsLambdaJavaCore = "1.2.1"
val awsLambdaJavaEvents = "3.10.0"
val junitVersion = "4.13.2"
val bouncyCastleVersion = "1.69"
val vavrVersion = "0.10.4"
val immutablesValueVersion = "2.8.8"
val awsIotCoreWebsocketsVersion = "3.0.1"
val log4jVersion = "2.14.1"
val awsLambdaJavaLog4j2Version = "1.2.0"
val awsCdkConstructsForJavaVersion = "0.16.14"
val resultsIteratorForAwsJavaSdkVersion = "29.0.18"
val awsCdkVersion = "1.121.0"
val daggerVersion = "2.38.1"

configurations.all {
    // Check for updates on changing dependencies at most every 10 minutes
    resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
}

group = "local"
version = "1.0-SNAPSHOT"

dependencies {
    // Dagger code generation
    annotationProcessor("com.google.dagger:dagger-compiler:$daggerVersion")

    // Dependency injection with Dagger
    implementation("com.google.dagger:dagger:$daggerVersion")

    annotationProcessor("org.immutables:value:$immutablesValueVersion")
    annotationProcessor("org.immutables:gson:$immutablesValueVersion")
    implementation("org.immutables:value:$immutablesValueVersion")

    implementation("com.igormaznitsa:jbbp:$jbbpVersion")

    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    api("org.apache.logging.log4j:log4j-slf4j18-impl:$log4jVersion")
    api("com.amazonaws:aws-lambda-java-log4j2:$awsLambdaJavaLog4j2Version")
    implementation("software.amazon.awssdk:sts:$awsSdk2Version")
    implementation("software.amazon.awssdk:cloudwatch:$awsSdk2Version")
    implementation("software.amazon.awssdk:apache-client:$awsSdk2Version")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-mqtt:$vertxVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:$pahoMqttv3Version")
    implementation("com.amazonaws:aws-lambda-java-core:$awsLambdaJavaCore")
    implementation("com.amazonaws:aws-lambda-java-events:$awsLambdaJavaEvents")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("io.vavr:vavr:$vavrVersion")
    implementation("com.github.awslabs:aws-iot-core-websockets:$awsIotCoreWebsocketsVersion")

    implementation("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJavaVersion")
    implementation("com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion")

    api("software.amazon.awscdk:ecs:$awsCdkVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("io.vertx:vertx-unit:$vertxVersion")
    // To avoid netty native DNS resolver for MacOS avoids this warning but is not required:
    // DnsServerAddressStreamProviders - Can not find io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider in the classpath, fallback to system defaults. This may result in incorrect DNS resolutions on MacOS.
    testImplementation("io.netty:netty-resolver-dns-native-macos:4.1.67.Final:osx-x86_64")
}

task("synth", JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    main = "com.awssamples.fargate.IotCoreProxyStack"
}
