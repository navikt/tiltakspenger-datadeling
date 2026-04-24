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
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.4.0")
}

