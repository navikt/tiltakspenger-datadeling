// `kotlin-dsl` (versjonen følger Gradle-distribusjonen) drar kotlin-gradle-plugin 2.4.0 med CVE-2026-53914
// (usikker deserialisering i build cache) inn på buildSrc' egen buildscript-classpath. Den classpathen nås
// ikke av `plugins`-blokka i hovedbygget, så versjonen må løftes her. Hold i sync med `kotlin("jvm")` i
// build.gradle.kts; kan fjernes når Gradle selv leverer en kotlin-dsl bygget på Kotlin >= 2.4.20
// (sjekk med `./gradlew -p buildSrc buildEnvironment`). Samme grep som i tiltakspenger-libs' build-logic.
buildscript {
    dependencies {
        constraints {
            add("classpath", "org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
        }
    }
}

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // Spotless må være på runtime-classpath her fordi [FlowStilNullableUnion]
    // bruker [com.diffplug.spotless.FormatterFunc]. Ved å bruke `implementation`
    // blir plugin-en gjort tilgjengelig for hovedprosjektets build.gradle.kts
    // uten at vi må spesifisere versjon i `plugins`-blokken der (versjonen er
    // allerede pinned her).
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.2")
}

