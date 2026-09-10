package no.nav.tiltakspenger.datadeling.infra

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Bakgrunnsprosessoppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Jobboppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.KafkaConsumerOppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Miljøverdi
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.libs.ktor.common.oppstart.prometheusMeterRegistry
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import java.time.Clock
import kotlin.time.Duration.Companion.minutes

const val CALL_ID_MDC_KEY = "call-id"

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile)
    val log = KotlinLogging.logger {}

    start(log = log)
}

/**
 * Komposisjonsroten.
 * Her konstrueres registeret alle appens målinger registreres i: Ktor-metrikkene, jobbmålingene og meldingsleser-målingene.
 * Det er det samme registeret `/metrics` skraper, så sender vi inn et annet register ett av stedene, forsvinner seriene stille.
 * Registeret lages av `prometheusMeterRegistry()` fra libs, som binder det til Prometheus sitt globale register; se KDoc-en der.
 * Tester lager sitt eget register, fordi et prosessnavn bare kan registreres én gang per register.
 */
fun start(
    log: KLogger,
    applicationContext: ApplicationContext = ApplicationContext(
        clock = Clock.system(zoneIdOslo),
        meterRegistry = prometheusMeterRegistry(),
    ),
    port: Int = Configuration.httpPort,
    host: String = "0.0.0.0",
    isNais: Boolean = Configuration.isNais(),
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    startApp(
        log = log,
        port = port,
        host = host,
        isNais = isNais,
        oppsett = bakgrunnsprosessoppsett(applicationContext = applicationContext, isNais = isNais),
    ) { readiness ->
        ktorSetup(
            applicationContext = applicationContext,
            readiness = readiness,
            visSwagger = Configuration.isDev(),
        )
    }
}

/**
 * Bakgrunnsprosessene appen kjører: jobben som sender vedtak til OBO, og consumeren som leser identhendelser.
 * Funksjonen ligger i komposisjonsroten fordi lista er komposisjonsrotens: det er her det avgjøres hva appen faktisk starter.
 * Wiring-testen kaller den for å starte de samme jobbene som produksjon, slik at målingene den sjekker er de ekte.
 */
fun bakgrunnsprosessoppsett(applicationContext: ApplicationContext, isNais: Boolean): Bakgrunnsprosessoppsett =
    Bakgrunnsprosessoppsett(
        jobber = Jobboppsett(
            mdcCallIdKey = CALL_ID_MDC_KEY,
            electorPath = Configuration::electorPath,
            clock = applicationContext.clock,
            meterRegistry = applicationContext.meterRegistry,
            tasks = if (isNais) {
                listOf(
                    Task(
                        navn = "send-til-obo",
                        intervall = Miljøverdi.lik(1.minutes),
                        utfør = { _ ->
                            applicationContext.sendTilOboService.send()
                            TaskResultat.Ferdig
                        },
                    ),
                )
            } else {
                emptyList()
            },
        ),
        kafkaConsumers = if (isNais) {
            listOf(
                KafkaConsumerOppsett(
                    navn = "identhendelse-consumer",
                    start = { applicationContext.identhendelseConsumer.run() },
                    stopp = { applicationContext.identhendelseConsumer.stop() },
                ),
            )
        } else {
            emptyList()
        },
    )
