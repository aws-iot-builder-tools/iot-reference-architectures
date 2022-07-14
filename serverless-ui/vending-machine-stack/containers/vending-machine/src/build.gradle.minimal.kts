plugins {
    kotlin("jvm") version "1.4.30"
}

val gradleDependencyVersion = "7.0"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.BIN
}

repositories {
    mavenCentral()
    google()
}

dependencies {
}
