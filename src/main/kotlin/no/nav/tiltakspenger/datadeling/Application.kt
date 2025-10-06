package no.nav.tiltakspenger.datadeling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import no.nav.tiltakspenger.datadeling.Configuration.httpPort
import no.nav.tiltakspenger.datadeling.application.auth.systembrukerMapper
import no.nav.tiltakspenger.datadeling.application.db.DataSourceSetup
import no.nav.tiltakspenger.datadeling.application.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.behandling.datadeling.BehandlingService
import no.nav.tiltakspenger.datadeling.behandling.datadeling.routes.behandlingRoutes
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.motta.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseConsumer
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseService
import no.nav.tiltakspenger.datadeling.meldekort.db.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.routes.mottaRoutes
import no.nav.tiltakspenger.datadeling.routes.swaggerRoute
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.VedtakService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.motta.MottaNyttVedtakService
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.TexasAuthenticationProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import java.time.Clock

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())
    val log = KotlinLogging.logger {}
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    val server = embeddedServer(Netty, port = httpPort(), module = { this.module(log, Clock.system(zoneIdOslo)) })

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 10_000)
        },
    )
    server.start(wait = true)
}

fun Application.module(log: KLogger, clock: Clock) {
    val texasClient: TexasClient = TexasHttpClient(
        introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
        tokenUrl = Configuration.naisTokenEndpoint,
        tokenExchangeUrl = Configuration.tokenExchangeEndpoint,
    )

    val dataSource = DataSourceSetup.createDatasource(Configuration.jdbcUrl)
    val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)

    val arenaClient = ArenaClient(
        baseUrl = Configuration.arenaUrl,
        getToken = { texasClient.getSystemToken(Configuration.arenaScope, IdentityProvider.AZUREAD, rewriteAudienceTarget = false) },
    )

    val behandlingRepo = BehandlingRepo(sessionFactory)
    val vedtakRepo = VedtakRepo(sessionFactory)
    val meldeperiodeRepo = MeldeperiodeRepo(sessionFactory)

    val vedtakService = VedtakService(vedtakRepo, arenaClient)
    val behandlingService = BehandlingService(behandlingRepo)

    val mottaNyttVedtakService = MottaNyttVedtakService(vedtakRepo)
    val mottaNyBehandlingService = MottaNyBehandlingService(behandlingRepo)

    val identhendelseService = IdenthendelseService(behandlingRepo, vedtakRepo, meldeperiodeRepo)
    val identhendelseConsumer = IdenthendelseConsumer(
        identhendelseService = identhendelseService,
        topic = Configuration.identhendelseTopic,
    )

    jacksonSerialization()
    configureExceptions()
    setupAuthentication(texasClient)
    routing {
        // Hver route står for sin egen autentisering og autorisering.
        healthRoutes()
        if (Configuration.applicationProfile() == Profile.DEV) {
            swaggerRoute()
        }
        authenticate(IdentityProvider.AZUREAD.value) {
            vedtakRoutes(vedtakService)
            behandlingRoutes(behandlingService)
            mottaRoutes(mottaNyttVedtakService, mottaNyBehandlingService, clock, meldeperiodeRepo)
        }
    }

    if (Configuration.isNais()) {
        val consumers = listOf(
            identhendelseConsumer,
        )
        consumers.forEach { it.run() }
    }

    attributes.put(isReadyKey, true)
}

fun Application.setupAuthentication(texasClient: TexasClient) {
    authentication {
        register(
            TexasAuthenticationProvider(
                TexasAuthenticationProvider.Config(
                    name = IdentityProvider.AZUREAD.value,
                    texasClient = texasClient,
                    identityProvider = IdentityProvider.AZUREAD,
                ),
            ),
        )
    }
}

fun Application.configureExceptions() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            ExceptionHandler.handle(call, cause)
        }
    }
}

// Vi må la ktor styre serialisering av responser for å kunne generere openapi-skjema
fun Application.jacksonSerialization() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
        }
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

val isReadyKey = AttributeKey<Boolean>("isReady")

fun Application.isReady() = attributes.getOrNull(isReadyKey) == true
