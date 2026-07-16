import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import openapi.FlowStilNullableUnion
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.io.StringWriter

val ktorVersjon = "3.4.3"
val testContainersVersion = "2.0.5"
val felleslibVersion = "0.0.863"

buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.yaml:snakeyaml:2.6")
    }
}

plugins {
    application
    kotlin("jvm") version "2.4.0"
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
    // Lås versjonene på alle Kotlin-komponenter til samme versjon
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))

    // Lås alle io.netty:* til samme versjon. r2dbc-postgresql/reactor-netty (transitiv via
    // persistering-infrastruktur) drar inn netty 4.1.x, mens ktor-server-netty bruker 4.2.x.
    // Uten dette havner både netty-codec (4.1) og netty-codec-base (4.2) på classpath med
    // duplikate baseklasser (ByteToMessageDecoder m.fl.), som med `-cp lib/*` lastes i feil
    // rekkefølge og brekker HTTP-pipelinen.
    implementation(platform("io.netty:netty-bom:4.2.15.Final"))
    implementation("ch.qos.logback:logback-classic:1.5.37")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("org.jetbrains:annotations:26.1.0")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.arrow-kt:arrow-core:2.2.3")

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
    implementation("org.flywaydb:flyway-database-postgresql:12.10.0")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.github.seratch:kotliquery:1.9.1")

    testImplementation(platform("org.junit:junit-bom:6.1.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("reflect"))
    testImplementation("org.yaml:snakeyaml:2.6")
    testImplementation("io.swagger.parser.v3:swagger-parser:2.1.45")
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

configurations.all {
    // ekskluder JUnit 4
    exclude(group = "junit", module = "junit")
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
        jvmToolchain(25)
        compilerOptions {
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
    }

    test {
        // JUnit 5-støtte
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
        testLogging {
            // Vi logger bare feilede og hoppede tester når Gradle kjører.
            events("skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    register("checkFlywayMigrationNames") {
        val sqlMigrationDir = project.file("src/main/resources/db/migration")
        val kotlinMigrationDir = project.file("src/main/kotlin/db/migration")
        doLast {
            val sqlFiles =
                sqlMigrationDir
                    .walk()
                    .filter { it.isFile && it.extension == "sql" }
                    .toList()

            val invalidSqlFiles =
                sqlFiles
                    .filterNot { it.name.matches(Regex("V[0-9]+__[a-zA-Z0-9][\\w]+\\.sql")) }
                    .map { it.name }

            if (invalidSqlFiles.isNotEmpty()) {
                throw GradleException("Invalid SQL migration filenames:\n${invalidSqlFiles.joinToString("\n")}")
            }
            val kotlinFiles =
                kotlinMigrationDir
                    .walk()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                    .toList()

            val invalidKotlinFiles =
                kotlinFiles
                    .filterNot { it.name.matches(Regex("V[0-9]+__[a-zA-Z0-9][\\w]+\\.(kt|java)")) }
                    .map { it.name }

            if (invalidKotlinFiles.isNotEmpty()) {
                throw GradleException("Invalid Kotlin/Java migration filenames:\n${invalidKotlinFiles.joinToString("\n")}")
            }

            // Sjekk for dupliserte versjoner på tvers av ALLE migreringstyper
            val allFiles = sqlFiles + kotlinFiles
            val duplicateVersions =
                allFiles
                    .mapNotNull {
                        it.name
                            .split("__")
                            .firstOrNull()
                            ?.removePrefix("V")
                            ?.toIntOrNull()
                    }.groupBy { it }
                    .filter { it.value.size > 1 }
                    .keys

            if (duplicateVersions.isNotEmpty()) {
                throw GradleException(
                    "Duplicate version numbers found:\n${duplicateVersions.joinToString("\n") { "Version $it is used multiple times" }}",
                )
            }

            println("All migration filenames are valid and version numbers are unique.")
        }
    }

    register<Copy>("gitHooks") {
        group = "git hooks"
        description = "Installerer git-hooks fra .gitHooks/ til .git/hooks/."
        from(file(".gitHooks"))
        into(file(".git/hooks"))
        filePermissions { unix("rwxr-xr-x") }
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
// Holder 100 % linjedekning for utvalgte pakker og klasser. Dekningen rapporteres
// som HTML/XML på `check`, og bygget feiler hvis terskelen ikke holdes.
kover {
    reports {
        total {
            filters {
                includes {
                    // TODO jah: Vurder om Kover-låsen på private route-/DTO-klasser er for skjør ved refaktorering/navneendringer.
                    classes(
                        "no.nav.tiltakspenger.datadeling.behandling.BehandlingService",
                        "no.nav.tiltakspenger.datadeling.behandling.infra.routes.BehandlingRoutesKt*",
                        "no.nav.tiltakspenger.datadeling.behandling.infra.routes.BehandlingRequestDTO",
                        "no.nav.tiltakspenger.datadeling.behandling.infra.routes.TpsakBehandlingResponseDTO*",
                        "no.nav.tiltakspenger.datadeling.vedtak.infra.routes.HentSakRouteKt*",
                        "no.nav.tiltakspenger.datadeling.vedtak.infra.routes.HentSakResponseDTO",
                        "no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakTidslinjeResponse*",
                        "no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakTidslinjeSakDTO",
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
                rule("utvalgte pakker og klasser skal ha 100 % linjedekning") {
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

