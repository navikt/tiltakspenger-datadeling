package no.nav.tiltakspenger.datadeling.infra

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.infra.auth.systembrukerMapper
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Bakgrunnsprosessoppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.KafkaConsumerOppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Miljøverdi
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import java.time.Clock
import kotlin.time.Duration.Companion.minutes

const val CALL_ID_MDC_KEY = "call-id"

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())
    val log = KotlinLogging.logger {}

    start(log = log)
}

fun start(
    log: KLogger,
    applicationContext: ApplicationContext = ApplicationContext(
        clock = Clock.system(zoneIdOslo),
    ),
    port: Int = Configuration.httpPort(),
    isNais: Boolean = Configuration.isNais(),
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    startApp(
        log = log,
        port = port,
        isNais = isNais,
        oppsett = Bakgrunnsprosessoppsett(
            mdcCallIdKey = CALL_ID_MDC_KEY,
            electorPath = Configuration::electorPath,
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
            kafkaConsumers = if (isNais) {
                listOf(
                    KafkaConsumerOppsett(
                        navn = "identhendelse-consumer",
                        start = { applicationContext.identhendelseConsumer.run() },
                        stopp = {},
                    ),
                )
            } else {
                emptyList()
            },
            clock = applicationContext.clock,
        ),
    ) { readiness ->
        ktorSetup(applicationContext = applicationContext, readiness = readiness)
    }
}

/**
 * Brukes for å mappe verifisert systembruker-token til Systembruker
 */
@Suppress("UNCHECKED_CAST")
fun getSystemBrukerMapper() = ::systembrukerMapper as (String, String, Set<String>) -> GenerellSystembruker<
    GenerellSystembrukerrolle,
    GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    >
