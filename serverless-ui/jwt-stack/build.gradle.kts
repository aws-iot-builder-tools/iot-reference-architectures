import groovy.lang.Closure
import org.akhikhl.gretty.GrettyExtension
import org.wisepersist.gradle.plugins.gwt.GwtCompileOptions
import org.wisepersist.gradle.plugins.gwt.GwtPluginExtension
import org.wisepersist.gradle.plugins.gwt.GwtSuperDevOptions

plugins {
    kotlin("jvm") version "2.2.21"
    id("application")
    id("java")
    id("idea")
    id("java-library")
    id("war")

    // Creates fat JAR
    id("com.github.johnrengelman.shadow") version "8.1.1"

    id("org.wisepersist.gwt") version "1.1.19"
    id("org.gretty") version "4.1.10"

    id("com.github.ben-manes.versions") version "0.52.0"
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

idea.module.isDownloadSources = true
idea.module.isDownloadJavadoc = true

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

// Required if using Dagger (and other annotations that generate code)
sourceSets {
    val main by getting
    main.java.srcDirs("build/generated/sources/annotationProcessor/java/main")
}

// Required for shadow JAR but we don't use it. Can not be replaced with application.mainClass.set.
application.mainClassName = "not-necessary"

val gradleDependencyVersion = "7.2"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

repositories {
    mavenCentral()
    mavenLocal()
    google()
    // Required for gretty
    jcenter()
    // Required for AWS Lambda Servlet annotation processor
    maven(url = "https://jitpack.io")
    // Required for Gradle Tooling API
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
}

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }
tasks.shadowDistZip { enabled = false }
tasks.shadowDistTar { enabled = false }

val awsLambdaJavaCoreVersion = "1.3.0"
val awsLambdaJavaLog4j2Version = "1.6.0"
val jacksonVersion = "2.19.2"
val awsSdk2Version = "2.36.3"
val vavrVersion = "0.10.7"
val vavrJacksonVersion = "0.10.3"
val vavrGwtVersion = "0.9.2"
val gwtServletVersion = "2.10.0"
val junitVersion = "4.13.2"
// NOTE: Upgrading Jetty to 10.0.0 or beyond will cause this error - java.lang.NoSuchMethodError: 'void org.eclipse.jetty.server.ServerConnector.setSoLingerTime(int)'
val jettyVersion = "11.0.26"
val bouncyCastleVersion = "1.70"
val jjwtVersion = "4.5.0"
val vertxVersion = "5.0.5"
val awsCdkConstructsForJava = "0.20.0"
val awsCdkVersion = "1.156.1"
val awsLambdaServletVersion = "0.3.8"
val log4jVersion = "2.18.0"
val daggerVersion = "2.57"
val gwtMaterialVersion = "2.8.5"
val elemental2Version = "1.3.2"
val elementoVersion = "2.3.0"
val gwtJacksonVersion = "0.15.4"
val resultsIteratorForAwsJavaSdkVersion = "29.0.25"

dependencies {
    // Dagger code generation
    annotationProcessor("com.google.dagger:dagger-compiler:$daggerVersion")
    implementation("com.google.dagger:dagger:$daggerVersion")
    implementation("com.google.dagger:dagger-gwt:$daggerVersion")

    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("io.vavr:vavr:$vavrVersion")
    implementation("io.vavr:vavr-jackson:$vavrJacksonVersion")
    implementation("io.vavr:vavr-gwt:$vavrGwtVersion")
    implementation("software.amazon.awssdk:iot:$awsSdk2Version")
    implementation("software.amazon.awssdk:iotdataplane:$awsSdk2Version")
    implementation("software.amazon.awssdk:sts:$awsSdk2Version")
    implementation("com.google.gwt:gwt-servlet:$gwtServletVersion")
    implementation("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-server:$jettyVersion")

    // Lambda core and logging
    implementation("com.amazonaws:aws-lambda-java-core:$awsLambdaJavaCoreVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    api("org.apache.logging.log4j:log4j-slf4j18-impl:$log4jVersion")
    api("com.amazonaws:aws-lambda-java-log4j2:$awsLambdaJavaLog4j2Version")

    implementation("com.github.gwtmaterialdesign:gwt-material:$gwtMaterialVersion")
    implementation("com.github.gwtmaterialdesign:gwt-material-themes:$gwtMaterialVersion")
    // GWT material addins includes GWT material jquery
    implementation("com.github.gwtmaterialdesign:gwt-material-addins:$gwtMaterialVersion")
    implementation("com.github.gwtmaterialdesign:gwt-material-table:$gwtMaterialVersion")

    implementation("com.google.elemental2:elemental2-dom:$elemental2Version")
    implementation("org.jboss.elemento:elemento-core:$elementoVersion")
    implementation("com.github.nmorel.gwtjackson:gwt-jackson:$gwtJacksonVersion")

    implementation("com.auth0:java-jwt:$jjwtVersion")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")

    api("com.github.aws-samples:aws-lambda-servlet:$awsLambdaServletVersion")
    annotationProcessor("com.github.aws-samples:aws-lambda-servlet:$awsLambdaServletVersion")

    implementation("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJava")
    annotationProcessor("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJava")

    implementation("com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion")
    implementation("software.amazon.awssdk:apache-client:$awsSdk2Version")

    testImplementation("junit:junit:$junitVersion")
}

configurations.all {
    // Check for updates on changing dependencies at most every 10 minutes
    resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)

    // Don't allow any configuration to use a broken version of HTTP client
    resolutionStrategy.force("org.apache.httpcomponents:httpclient:4.5.14")
}

configurations.implementation {
    // Fixes - Caused by: java.util.ServiceConfigurationError: org.apache.juli.logging.Log: org.eclipse.jetty.apache.jsp.JuliLog not a subtype
    exclude("org.mortbay.jasper")
}

// Extension configuration
configure<GrettyExtension> {
    // Supported values:
    // "jetty7", "jetty8", "jetty9", "tomcat7", "tomcat8"
    servletContainer = "jetty9.4"
}

configure<GwtPluginExtension> {
    gwtVersion = gwtServletVersion
    maxHeapSize = "2048M"

//    modules("com.awslabs.iatt.spe.serverless.gwt.Jwt")
    modules("com.awssamples.Jwt")

    compiler(closureOf<GwtCompileOptions> {
        localWorkers = 8
        optimize = 9
        maxHeapSize = "2048M"
        // Set this value if you need to debug the generated source
        // style = Style.PRETTY
    } as Closure<GwtCompileOptions>)

    superDev(closureOf<GwtSuperDevOptions> {
        noPrecompile = true
        maxHeapSize = "2048M"
    } as Closure<GwtSuperDevOptions>)
}

// Task configuration
val awsIotBrowserBundle = "aws-iot-sdk-browser-bundle-min.js"
val awsIotBrowserBundleRelativeDestination = "./war/" + awsIotBrowserBundle
val awsIotBrowserBundleAbsoluteDestination =
    project.projectDir.absolutePath + "/" + awsIotBrowserBundleRelativeDestination

val copyBrowserBundle by tasks.registering(Exec::class) {
    dependsOn += createDockerContainerForBrowserBundle

    commandLine(
        "docker",
        "cp",
        "aws-iot-device-sdk-js-container:/node_modules/aws-iot-device-sdk/browser/aws-iot-sdk-browser-bundle-min-uglifyjs.js",
        awsIotBrowserBundleRelativeDestination
    )
}

tasks.warTemplate {
    if (!File(awsIotBrowserBundleAbsoluteDestination).exists()) {
        // If we don't have the browser bundle we'll need to build and copy it before the war template task runs
        dependsOn += copyBrowserBundle
    }
}

tasks.war {
    // Make sure GWT code is compiled before the war is generated
    dependsOn += tasks.compileGwt

    // Make sure the fixed keys are generated before the WAR
    dependsOn += tasks.test
}

tasks.shadowJar {
    // To prevent broken log4j2 configurations - see https://stackoverflow.com/a/61475766
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)

    // Wait until the war is generated so we can get the files from its output directory
    dependsOn += tasks.war
    dependsOn += tasks.warTemplate

    // Make sure the fixed keys are generated before the shadow JAR
    dependsOn += tasks.test

    // Get all of the GWT compiler output
    inputs.files(fileTree("build/gwt/out"))
    from("build/gwt/out")

    // Get all of the static assets
    inputs.files(fileTree("war"))
    from("war")

    // Exclude WEB-INF so we don't include a bunch of classes we don't need
    exclude("WEB-INF")

    dependencies {
        // NOTE: Do not exclude the following dependencies or things will break
        // - javax.servlet:.*
        // - com.google.gwt:gwt-servlet:.*
        exclude(dependency("org.gwtproject.i18n:.*"))
        exclude(dependency("org.gwtproject.core:.*"))
        exclude(dependency("org.gwtproject.editor:.*"))
        exclude(dependency("org.gwtproject.timer:.*"))
        exclude(dependency("org.gwtproject.event:.*"))
        exclude(dependency("org.gwtproject.safehtml:.*"))
        exclude(dependency("io.vertx:.*"))
        exclude(dependency("net.sourceforge.htmlunit:.*"))
        exclude(dependency("colt:.*"))
        exclude(dependency("com.github.tdesjardins:.*"))
        exclude(dependency("tapestry:.*"))
        exclude(dependency("com.google.gwt:gwt-dev:.*"))
        exclude(dependency("com.google.gwt:gwt-user:.*"))
        exclude(dependency("net.sourceforge.cssparser:.*"))
        exclude(dependency("com.ibm.icu:.*"))
        exclude(dependency("javax.websocket:.*"))
        exclude(dependency("org.glassfish:.*"))

        // Removed dependencies from the results iterator that we don't need
        exclude(dependency("software.amazon.awssdk:dynamodb:.*"))
        exclude(dependency("software.amazon.awssdk:ec2:.*"))
    }

    isZip64 = true
}

val validateDockerIsAvailable by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    commandLine("bash", "-c", "docker &> /dev/null")

    doLast {
        if (execResult!!.exitValue != 0) {
            throw GradleException("Docker is required for this build")
        }
    }
}

val validateUserHasPermissionsToRunDocker by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    dependsOn += validateDockerIsAvailable
    commandLine("bash", "-c", "docker ps &> /dev/null")

    doLast {
        if (execResult!!.exitValue != 0) {
            throw GradleException(
                "This user does not have permission to use Docker or Docker is not running. " +
                        "If you recently added this user to the Docker group try logging out and logging back in again. " +
                        "If you are still unable to run this script but can run 'docker ps' in your shell try killing any existing Gradle daemons that were started before the user was added to the Docker group."
            )

        }
    }
}

val buildDockerImageForBrowserBundle by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    dependsOn += validateUserHasPermissionsToRunDocker
    commandLine("docker", "build", "--target", "awsIotDeviceSdk", "-t", "aws-iot-device-sdk-js-build", ".")

    doLast {
        if (execResult!!.exitValue != 0) {
            throw GradleException("The docker build of the Javascript IoT device SDK failed")
        }
    }
}

val removeBrowserBundleContainerBefore by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    commandLine("docker", "rm", "-f", "/aws-iot-device-sdk-js-container")
}

val createDockerContainerForBrowserBundle by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    dependsOn += buildDockerImageForBrowserBundle
    dependsOn += removeBrowserBundleContainerBefore

    commandLine(
        "docker",
        "create",
        "-ti",
        "--name",
        "aws-iot-device-sdk-js-container",
        "aws-iot-device-sdk-js-build:latest",
        "bash"
    )

    doLast {
        if (execResult!!.exitValue != 0) {
            throw GradleException("Creating the container to extract the Javascript IoT device SDK failed")
        }
    }
}

task("synth", JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    main = "com.awssamples.serverlessui.JwtStack"
}
