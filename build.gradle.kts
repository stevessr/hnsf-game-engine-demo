plugins {
    java
    application
    id("io.freefair.lombok") version "8.14.2"
    id("checkstyle")
    id("com.github.spotbugs") version "6.4.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("org.json:json:20250517")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

checkstyle {
    toolVersion = "10.26.1"
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(layout.projectDirectory.file("config/spotbugs/spotbugs-exclude.xml"))
    ignoreFailures = true
    reports.create("html") {
        required.set(true)
    }
    reports.create("xml") {
        required.set(false)
    }
}

application {
    mainClass = "org.example.App"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.example.App"
    }
}
