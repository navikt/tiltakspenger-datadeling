val javaVersjon = JavaVersion.VERSION_21
val mockkVersjon = "1.13.8"
val ktorVersjon = "2.3.11"
val jacksonVersjon = "2.16.1"
val kotestVersjon = "5.8.0"
val tokenSupportVersjon = "3.2.0"
val testContainersVersion = "1.19.4"
val kotlinxCoroutinesVersion = "1.7.3"

plugins {
    application
    kotlin("jvm") version "1.9.22"
    id("com.diffplug.spotless") version "6.23.3"
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
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.natpryce:konfig:1.6.10.0")

    implementation("io.arrow-kt:arrow-core:1.2.1")

    implementation("io.ktor:ktor-server-netty:$ktorVersjon")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersjon")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersjon")
    implementation("io.ktor:ktor-server-call-id:$ktorVersjon")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersjon")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersjon")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersjon")

    // TokenX
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersjon")
    implementation("no.nav.security:token-client-core:$tokenSupportVersjon")

    implementation("io.ktor:ktor-client-core:$ktorVersjon")
    implementation("io.ktor:ktor-client-cio:$ktorVersjon")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersjon")
    implementation("io.ktor:ktor-client-logging:$ktorVersjon")
    implementation("io.ktor:ktor-http:$ktorVersjon")
    implementation("io.ktor:ktor-serialization:$ktorVersjon")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersjon")
    implementation("io.ktor:ktor-utils:$ktorVersjon")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersjon")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersjon")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:$mockkVersjon")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersjon")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersjon")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersjon")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersjon")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersjon")
    testImplementation("io.kotest:kotest-extensions:$kotestVersjon")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    // need quarkus-junit-4-mock because of https://github.com/testcontainers/testcontainers-java/issues/970
//    testImplementation("io.quarkus:quarkus-junit4-mock:3.7.0")
}

configurations.all {
    // exclude JUnit 4
    exclude(group = "junit", module = "junit")
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
    compileKotlin {
        kotlinOptions.jvmTarget = javaVersjon.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = javaVersjon.toString()
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
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
