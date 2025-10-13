package no.nav.tiltakspenger.datadeling

import arrow.core.Either
import arrow.core.right
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey
import no.nav.tiltakspenger.datadeling.Configuration.httpPort
import no.nav.tiltakspenger.datadeling.application.auth.systembrukerMapper
import no.nav.tiltakspenger.datadeling.application.context.ApplicationContext
import no.nav.tiltakspenger.datadeling.application.jobber.TaskExecutor
import no.nav.tiltakspenger.datadeling.application.ktorSetup
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import java.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

const val CALL_ID_MDC_KEY = "call-id"

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())
    val log = KotlinLogging.logger {}

    start(log = log)
}

fun start(
    log: KLogger,
    applicationContext: ApplicationContext = ApplicationContext(
        log = log,
        clock = Clock.system(zoneIdOslo),
    ),
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    val server = embeddedServer(
        factory = Netty,
        port = httpPort(),
        module = { ktorSetup(applicationContext) },
    )
    server.application.attributes.put(isReadyKey, true)

    val runCheckFactory = if (Configuration.isNais()) {
        RunCheckFactory(
            leaderPodLookup =
            LeaderPodLookupClient(
                electorPath = Configuration.electorPath(),
                logger = KotlinLogging.logger { },
            ),
            attributes = server.application.attributes,
            isReadyKey = isReadyKey,
        )
    } else {
        RunCheckFactory(
            leaderPodLookup =
            object : LeaderPodLookup {
                override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> =
                    true.right()
            },
            attributes = server.application.attributes,
            isReadyKey = isReadyKey,
        )
    }

    val jobber: TaskExecutor = TaskExecutor.startJob(
        initialDelay = if (Configuration.isNais()) 1.minutes else 1.seconds,
        runCheckFactory = runCheckFactory,
        tasks = listOf<suspend () -> Any>(
            { applicationContext.sendTilOboService.send() },
        ),
    )

    if (Configuration.isNais()) {
        val consumers = listOf(
            applicationContext.identhendelseConsumer,
        )
        consumers.forEach { it.run() }
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            jobber.stop()
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 10_000)
        },
    )
    server.start(wait = true)
}

/**
 * Brukes for å mappe verifisert systembruker-token til Systembruker
 */
@Suppress("UNCHECKED_CAST")
fun getSystemBrukerMapper() = ::systembrukerMapper as (String, String, Set<String>) -> GenerellSystembruker<
    GenerellSystembrukerrolle,
    GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    >

val isReadyKey = AttributeKey<Boolean>("isReady")

fun Application.isReady() = attributes.getOrNull(isReadyKey) == true
