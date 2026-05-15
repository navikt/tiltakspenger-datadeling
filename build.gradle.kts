import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import openapi.FlowStilNullableUnion
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.StringWriter

val javaVersjon = JavaVersion.VERSION_21
val ktorVersjon = "3.4.3"
val testContainersVersion = "2.0.4"
val felleslibVersion = "0.0.781"

buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.yaml:snakeyaml:2.6")
    }
}

plugins {
    application
    kotlin("jvm") version "2.3.21"
    // Versjon pinnes i buildSrc/build.gradle.kts
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
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
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.02")
    implementation("org.jetbrains:annotations:26.1.0")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.arrow-kt:arrow-core:2.2.2.1")

    // felles lib
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:ktor-common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:kafka:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:texas:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:satser:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:meldekort:${felleslibVersion}")
    implementation("com.github.navikt.tiltakspenger-libs:jobber:$felleslibVersion")

    // Ktor server
    implementation("io.ktor:ktor-serialization-jackson3:$ktorVersjon")
    implementation("io.ktor:ktor-server-call-id:$ktorVersjon")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersjon")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersjon")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersjon")
    implementation("io.ktor:ktor-server-core:$ktorVersjon")
    implementation("io.ktor:ktor-server-cors:$ktorVersjon")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersjon")
    implementation("io.ktor:ktor-server-netty:$ktorVersjon")
    implementation("io.ktor:ktor-serialization:$ktorVersjon")
    implementation("io.ktor:ktor-server-swagger:$ktorVersjon")
    implementation("io.ktor:ktor-server-auth:$ktorVersjon")

    // Ktor client
    implementation("io.ktor:ktor-client-core:$ktorVersjon")
    implementation("io.ktor:ktor-client-cio:$ktorVersjon")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersjon")
    implementation("io.ktor:ktor-client-logging:$ktorVersjon")

    // DB
    implementation("org.flywaydb:flyway-database-postgresql:12.6.0")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.postgresql:postgresql:42.7.11")
    implementation("com.github.seratch:kotliquery:1.9.1")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("reflect"))
    testImplementation("org.yaml:snakeyaml:2.6")
    testImplementation("io.swagger.parser.v3:swagger-parser:2.1.41")
    testImplementation("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:ktor-test-common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:auth-test-core:$felleslibVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersjon")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersjon")
    testImplementation("com.github.navikt.tiltakspenger-libs:persistering-test-common:$felleslibVersion")
    testImplementation("com.lemonappdev:konsist:0.17.3")
}

application {
    mainClass.set("no.nav.tiltakspenger.datadeling.infra.ApplicationKt")
}

java {
    sourceCompatibility = javaVersjon
    targetCompatibility = javaVersjon
}

spotless {
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_max-line-length" to "off",
                    "ktlint_standard_function-signature" to "disabled",
                    "ktlint_standard_function-expression-body" to "disabled",
                ),
            )
    }
    // Formaterer OpenAPI-kildefilene til kanonisk YAML (2 space-indent, ingen
    // tabs, konsistent quoting). `spotlessCheck` feiler hvis filene ikke er
    // formatert, og `spotlessApply` fikser dem.
    yaml {
        target("src/main/openapi/**/*.yaml")
        endWithNewline()
        trimTrailingWhitespace()
        jackson()
            .yamlFeature("WRITE_DOC_START_MARKER", false)
            .yamlFeature("MINIMIZE_QUOTES", true)
            // Unngå at Jackson bryter lange description-strenger på ~80 tegn
            // med "\\"-fortsettelse.
            .yamlFeature("SPLIT_LINES", false)
        // Kollapser nullability-union (`type:\n- <type>\n- "null"`) tilbake
        // til flow-stil etter at Jackson har block-formatert alt. Steget må
        // ligge i buildSrc som en serialiserbar named class – kotlin-lambdaer
        // og klasser definert i selve build.gradle.kts kan ikke fingerprint-es
        // av spotless.
        custom("flow-stil-nullable-union", FlowStilNullableUnion())
    }
}

tasks {
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
    }

    jar {
        dependsOn(configurations.runtimeClasspath)

        manifest {
            attributes["Main-Class"] = "no.nav.tiltakspenger.datadeling.infra.ApplicationKt"
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
        testLogging {
            // We only want to log failed and skipped tests when running Gradle.
            events("skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    register<Copy>("gitHooks") {
        description = "Kopierer pre-commit hook-skript til .git/hooks/ for å kjøre spotlessCheck før commit."
        from(file(".scripts/pre-commit"))
        into(file(".git/hooks"))
    }
}

// --- OpenAPI-bundling ---------------------------------------------------------
// Kildefilene ligger i src/main/openapi/ og kan splittes i flere filer via
// eksterne $ref. Under bygging blir de slått sammen til én documentation.yaml
// som havner på classpath som openapi/documentation.yaml (samme sti som Swagger
// UI i KtorSetup + SwaggerRoute forventer).
//
// Vi bruker snakeyaml direkte i stedet for swagger-parser fordi sistnevnte har
// bugs på OpenAPI 3.1 som stripper "nakne" `type: string`-felt og lignende ved
// cross-file-resolving (resolveFully=false). Bundleren her er en ren tekstlig
// sammenslåing: eksterne $ref-filer slås inn der de brukes, og cross-file
// $ref-er til components/schemas/<Name>.yaml rewrites til interne
// "#/components/schemas/<Name>"-referanser.
val openApiSourceDir = layout.projectDirectory.dir("src/main/openapi")
val openApiBundleRoot = layout.buildDirectory.dir("generated/openapi")

val bundleOpenApi by tasks.registering {
    group = "openapi"
    description = "Slår sammen src/main/openapi/**.yaml til én documentation.yaml"
    val inputRoot = openApiSourceDir.file("documentation.yaml")
    val outputFile = openApiBundleRoot.map { it.dir("openapi").file("documentation.yaml") }
    inputs.dir(openApiSourceDir)
    outputs.file(outputFile)
    doLast {
        val rootFile = inputRoot.asFile
        check(rootFile.exists()) { "Fant ikke OpenAPI-rotfil: $rootFile" }
        val yaml = org.yaml.snakeyaml.Yaml()

        fun loadYaml(file: File): Any? =
            file.inputStream().use { yaml.load(it) }

        // Gjør $ref interne ved å peke på #/components/schemas/<filnavn uten .yaml>.
        fun rewriteRefs(node: Any?): Any? = when (node) {
            is Map<*, *> -> {
                val ref = node[$$"$ref"]
                if (node.size == 1 && ref is String && ref.endsWith(".yaml")) {
                    val name = ref.substringAfterLast('/').removeSuffix(".yaml")
                    mapOf($$"$ref" to "#/components/schemas/$name")
                } else {
                    node.mapValues { (_, v) -> rewriteRefs(v) }
                }
            }

            is List<*> -> node.map { rewriteRefs(it) }
            else -> node
        }

        @Suppress("UNCHECKED_CAST")
        val root = loadYaml(rootFile) as MutableMap<String, Any?>

        // Inline paths/*.yaml
        @Suppress("UNCHECKED_CAST") val paths = root["paths"] as MutableMap<String, Any?>
        for ((p, value) in paths.toMap()) {
            val ref = (value as? Map<*, *>)?.get($$"$ref") as? String ?: continue
            val file = rootFile.parentFile.resolve(ref).normalize()
            paths[p] = rewriteRefs(loadYaml(file))
        }

        // Inline components.schemas/*.yaml
        @Suppress("UNCHECKED_CAST") val components = root["components"] as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST") val schemas = components["schemas"] as MutableMap<String, Any?>
        for ((name, value) in schemas.toMap()) {
            val ref = (value as? Map<*, *>)?.get($$"$ref") as? String ?: continue
            val file = rootFile.parentFile.resolve(ref).normalize()
            schemas[name] = rewriteRefs(loadYaml(file))
        }

        val dumperOptions = org.yaml.snakeyaml.DumperOptions().apply {
            defaultFlowStyle = org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
            splitLines = false
        }
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        val sw = StringWriter()
        org.yaml.snakeyaml.Yaml(dumperOptions).dump(root, sw)
        out.writeText(FlowStilNullableUnion.transformer(sw.toString()), Charsets.UTF_8)
    }
}

sourceSets.main {
    resources.srcDir(openApiBundleRoot)
}

tasks.named("processResources") { dependsOn(bundleOpenApi) }

// --- Kover --------------------------------------------------------------------
// Kopiert fra tiltakspenger-meldekort-api for å holde en minstedekning for
// utvalgte pakker. Dekningen rapporteres som HTML/XML på `check`, og bygget
// feiler hvis terskelen ikke holdes.
kover {
    reports {
        total {
            filters {
                includes {
                    classes(
                        "no.nav.tiltakspenger.datadeling.sak.**",
                    )
                }
            }
            html {
                onCheck = true
            }
            xml {
                onCheck = true
            }
            verify {
                onCheck = true
                rule("sak-pakken skal ha 100 % linjedekning") {
                    bound {
                        minValue = 100
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}

tasks.named("koverXmlReport") {
    val xmlReport = layout.buildDirectory.file("reports/kover/report.xml")
    doLast {
        val xml = xmlReport.get().asFile
        val classCount = xml.readText().split("<class ").size - 1
        if (classCount == 0) throw GradleException("Kover-rapporten inneholder ingen klasser – inkluderingsfilteret er trolig utdatert.")
    }
}

