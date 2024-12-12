package no.nav.tiltakspenger.datadeling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import mu.KLogger
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.Configuration.httpPort
import no.nav.tiltakspenger.datadeling.auth.systembrukerMapper
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClientImpl
import no.nav.tiltakspenger.datadeling.felles.app.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.motta.infra.db.BehandlingPostgresRepo
import no.nav.tiltakspenger.datadeling.motta.infra.db.DataSourceSetup
import no.nav.tiltakspenger.datadeling.motta.infra.db.VedtakPostgresRepo
import no.nav.tiltakspenger.datadeling.motta.infra.http.server.mottaRoutes
import no.nav.tiltakspenger.datadeling.routes.behandlingRoutes
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.datadeling.service.BehandlingService
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenHttpClient
import no.nav.tiltakspenger.libs.auth.core.MicrosoftEntraIdTokenService
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())
    val log = KotlinLogging.logger {}
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        sikkerlogg.error(e) { e.message }
    }

    embeddedServer(Netty, port = httpPort(), module = { this.module(log) }).start(wait = true)
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

    val arenaClient = ArenaClientImpl(getToken = { systemtokenClient.getSystemtoken(Configuration.arenaScope) })

    val behandlingRepo = BehandlingPostgresRepo(sessionFactory)
    val vedtakRepo = VedtakPostgresRepo(sessionFactory)

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
        vedtakRoutes(vedtakService, tokenService)
        behandlingRoutes(behandlingService, tokenService)
        mottaRoutes(mottaNyttVedtakService, mottaNyBehandlingService, tokenService)
    }
}

fun Application.configureExceptions() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            ExceptionHandler.handle(call, cause)
        }
    }
}

// TODO post-mvp jah: Fint å få slettet denne. Vi klarer serialisere/deserialisere selv.
fun Application.jacksonSerialization() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
        }
    }
}
