plugins {
    kotlin("jvm") version "1.6.10"
    id("java")
    id("application")
    id("com.bmuschko.docker-remote-api") version "6.7.0"
}

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }

// Required for shadow JAR plugin
application.mainClassName = "lambda.App"

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

val gradleDependencyVersion = "7.4.1"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.BIN
}

// To avoid this error in distTar:
//   > Entry results-iterator-for-aws-java-sdk-1.0-SNAPSHOT/lib/gson-2.9.0.jar is a duplicate but no duplicate handling strategy has been set. Please refer to https://docs.gradle.org/7.4.1/dsl/org.gradle.api.tasks.Copy.html#org.gradle.api.tasks.Copy:duplicatesStrategy for details.
//
// We follow the advice from https://github.com/gradle/gradle/issues/17236#issuecomment-870525957
gradle.taskGraph.whenReady {
    allTasks
        .filter { it.hasProperty("duplicatesStrategy") } // Because it's some weird decorated wrapper that I can't cast.
        .forEach {
            it.setProperty("duplicatesStrategy", "INCLUDE")
        }
}

repositories {
    mavenCentral()
    google()
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.gradle.org/gradle/libs-releases/")
}

val localLibsDirectory = "$rootDir/libs"

val resultsIteratorForAwsJavaSdkVersion = "29.0.25"
val ztZipVersion = "1.15"
val immutablesValueVersion = "2.9.0"
val immutablesVavrVersion = "0.6.2"
val junitVersion = "4.13.2"
val vavrVersion = "0.10.4"
val vavrJacksonVersion = "0.10.3"
val awsSdk2Version = "2.17.209"
val daggerVersion = "2.42"
val httpClientVersion = "4.5.13"
val gwtServletVersion = "2.9.0"
val guavaVersion = "31.1-jre"

dependencies {
    // Dagger code generation
    annotationProcessor("com.google.dagger:dagger-compiler:$daggerVersion")
    implementation("com.google.dagger:dagger:$daggerVersion")

    implementation("com.amazonaws:aws-lambda-java-runtime-interface-client:2.1.1")
    implementation(fileTree(localLibsDirectory))
    implementation("io.vavr:vavr:$vavrVersion")
    implementation("com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion")
    implementation("org.zeroturnaround:zt-zip:$ztZipVersion")

    implementation("com.google.gwt:gwt-servlet:$gwtServletVersion")

    // Immutables (requires annotation processing for code generation)
    annotationProcessor("org.immutables:value:$immutablesValueVersion")
    annotationProcessor("org.immutables:gson:$immutablesValueVersion")
    implementation("org.immutables:value:$immutablesValueVersion")
    implementation("org.immutables:gson:$immutablesValueVersion")

    // AWS SDKs
    implementation("software.amazon.awssdk:ssm:$awsSdk2Version")
    implementation("software.amazon.awssdk:sts:$awsSdk2Version")

    annotationProcessor("org.immutables.vavr:vavr-encodings:$immutablesVavrVersion")
    api("org.immutables.vavr:vavr-encodings:$immutablesVavrVersion")
    implementation("io.vavr:vavr-jackson:$vavrJacksonVersion")

    // Dependency added to fix - https://github.com/aws/aws-sdk-java-v2/issues/652
    implementation("org.apache.httpcomponents:httpclient:$httpClientVersion")
    implementation("software.amazon.awssdk:apache-client:$awsSdk2Version")

    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:$guavaVersion")

    testImplementation("junit:junit:$junitVersion")
}

val copyRuntimeDependencies = tasks.register("copyRuntimeDependencies", Copy::class) {
    from(configurations.runtimeClasspath)
    into("build/dependency")
}

tasks.build {
    dependsOn(copyRuntimeDependencies)
}

tasks.test {
    minHeapSize = "8g"
    maxHeapSize = "8g"
}
