import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val javaVersjon = JavaVersion.VERSION_21
val ktorVersjon = "3.0.1"
val testContainersVersion = "1.20.3"
val felleslibVersion = "0.0.294"

plugins {
    application
    kotlin("jvm") version "2.0.0"
    id("com.diffplug.spotless") version "6.25.0"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.arrow-kt:arrow-core:1.2.1")

    // felles lib
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:auth-core:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:auth-ktor:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:ktor-common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:datadeling-dtos:$felleslibVersion")

    // Ktor server
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersjon")
    implementation("io.ktor:ktor-server-call-id:$ktorVersjon")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersjon")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersjon")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersjon")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersjon")
    implementation("io.ktor:ktor-server-core:$ktorVersjon")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersjon")
    implementation("io.ktor:ktor-server-cors:$ktorVersjon")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersjon")
    implementation("io.ktor:ktor-server-host-common:$ktorVersjon")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersjon")
    implementation("io.ktor:ktor-server-netty:$ktorVersjon")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersjon")
    implementation("io.ktor:ktor-http-jvm:$ktorVersjon")

    // Ktor client
    implementation("io.ktor:ktor-client-core:$ktorVersjon")
    implementation("io.ktor:ktor-client-cio:$ktorVersjon")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersjon")
    implementation("io.ktor:ktor-client-logging:$ktorVersjon")
    implementation("io.ktor:ktor-http:$ktorVersjon")
    implementation("io.ktor:ktor-serialization:$ktorVersjon")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersjon")
    implementation("io.ktor:ktor-utils:$ktorVersjon")

    // DB
    implementation("org.flywaydb:flyway-database-postgresql:11.1.0")
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.github.seratch:kotliquery:1.9.0")

    testImplementation("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:ktor-test-common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:auth-test-core:$felleslibVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersjon")
    testImplementation("org.skyscreamer:jsonassert:1.5.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersjon")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")
}

application {
    mainClass.set("no.nav.tiltakspenger.datadeling.ApplicationKt")
}

java {
    sourceCompatibility = javaVersjon
    targetCompatibility = javaVersjon
}

spotless {
    kotlin {
        ktlint("0.48.2")
    }
}

tasks {
    kotlin {
        compileKotlin {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
        compileTestKotlin {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    jar {
        dependsOn(configurations.runtimeClasspath)

        manifest {
            attributes["Main-Class"] = "no.nav.tiltakspenger.datadeling.ApplicationKt"
            attributes["Class-Path"] = configurations.runtimeClasspath
                .get()
                .joinToString(separator = " ") { file -> file.name }
        }
    }

    test {
        // JUnit 5 support
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
    }
    /*
    analyzeClassesDependencies {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
    }
    analyzeTestClassesDependencies {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
    }
     */
}

task("addPreCommitGitHookOnBuild") {
    println("⚈ ⚈ ⚈ Running Add Pre Commit Git Hook Script on Build ⚈ ⚈ ⚈")
    exec {
        commandLine("cp", "./.scripts/pre-commit", "./.git/hooks")
    }
    println("✅ Added Pre Commit Git Hook Script.")
}
