package no.nav.tiltakspenger.datadeling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import no.nav.tiltakspenger.datadeling.Configuration.httpPort
import no.nav.tiltakspenger.datadeling.auth.systembrukerMapper
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.felles.app.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.motta.infra.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.motta.infra.db.DataSourceSetup
import no.nav.tiltakspenger.datadeling.motta.infra.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.motta.infra.http.server.mottaRoutes
import no.nav.tiltakspenger.datadeling.routes.behandling.behandlingRoutes
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.routes.swaggerRoute
import no.nav.tiltakspenger.datadeling.routes.vedtak.vedtakRoutes
import no.nav.tiltakspenger.datadeling.service.BehandlingService
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenHttpClient
import no.nav.tiltakspenger.libs.auth.core.MicrosoftEntraIdTokenService
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())
    val log = KotlinLogging.logger {}
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    val server = embeddedServer(Netty, port = httpPort(), module = { this.module(log) })

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 10_000)
        },
    )
    server.start(wait = true)
}

fun Application.module(log: KLogger) {
    val systemtokenClient: EntraIdSystemtokenClient = EntraIdSystemtokenHttpClient(
        baseUrl = Configuration.azureOpenidConfigTokenEndpoint,
        clientId = Configuration.azureAppClientId,
        clientSecret = Configuration.azureAppClientSecret,
    )

    val dataSource = DataSourceSetup.createDatasource(Configuration.jdbcUrl)
    val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)

    val arenaClient = ArenaClient(getToken = { systemtokenClient.getSystemtoken(Configuration.arenaScope) })

    val behandlingRepo = BehandlingRepo(sessionFactory)
    val vedtakRepo = VedtakRepo(sessionFactory)

    val vedtakService = VedtakService(vedtakRepo, arenaClient)
    val behandlingService = BehandlingService(behandlingRepo)

    val mottaNyttVedtakService = MottaNyttVedtakService(vedtakRepo)
    val mottaNyBehandlingService = MottaNyBehandlingService(behandlingRepo)

    @Suppress("UNCHECKED_CAST")
    val tokenService = MicrosoftEntraIdTokenService(
        url = Configuration.azureOpenidConfigJwksUri,
        issuer = Configuration.azureOpenidConfigIssuer,
        clientId = Configuration.azureAppClientId,
        autoriserteBrukerroller = emptyList(),
        systembrukerMapper = ::systembrukerMapper as (klientId: String, klientnavn: String, Set<String>) -> GenerellSystembruker<GenerellSystembrukerrolle, GenerellSystembrukerroller<GenerellSystembrukerrolle>>,
    )

    jacksonSerialization()
    configureExceptions()
    routing {
        // Hver route står for sin egen autentisering og autorisering.
        healthRoutes()
        if (Configuration.applicationProfile() == Profile.DEV) {
            swaggerRoute()
        }
        vedtakRoutes(vedtakService, tokenService)
        behandlingRoutes(behandlingService, tokenService)
        mottaRoutes(mottaNyttVedtakService, mottaNyBehandlingService, tokenService)
    }

    attributes.put(isReadyKey, true)
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

val isReadyKey = AttributeKey<Boolean>("isReady")

fun Application.isReady() = attributes.getOrNull(isReadyKey) == true
