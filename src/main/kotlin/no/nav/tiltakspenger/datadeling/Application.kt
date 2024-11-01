package no.nav.tiltakspenger.datadeling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import mu.KLogger
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.Configuration.httpPort
import no.nav.tiltakspenger.datadeling.auth.AzureTokenProvider
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClientImpl
import no.nav.tiltakspenger.datadeling.client.tp.TpClientImpl
import no.nav.tiltakspenger.datadeling.felles.app.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.felles.app.sikkerlogg
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.motta.infra.db.DataSourceSetup
import no.nav.tiltakspenger.datadeling.motta.infra.db.MottaNyttVedtakPostgresRepo
import no.nav.tiltakspenger.datadeling.motta.infra.http.server.mottaRoutes
import no.nav.tiltakspenger.datadeling.routes.behandlingRoutes
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.datadeling.service.BehandlingService
import no.nav.tiltakspenger.datadeling.service.VedtakService
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
    val tokenProviderVedtak = AzureTokenProvider(config = Configuration.oauthConfigVedtak())
    val tokenProviderArena = AzureTokenProvider(config = Configuration.oauthConfigArena())

    val vedtakClient = TpClientImpl(getToken = tokenProviderVedtak::getToken)
    val arenaClient = ArenaClientImpl(getToken = tokenProviderArena::getToken)

    val vedtakService = VedtakService(vedtakClient, arenaClient)
    val behandlingService = BehandlingService(vedtakClient)
    val dataSource = DataSourceSetup.createDatasource(Configuration.jdbcUrl)
    val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val mottaNyttVedtakRepo = MottaNyttVedtakPostgresRepo(sessionFactory)
    val mottaNyttVedtakService = MottaNyttVedtakService(mottaNyttVedtakRepo)

    jacksonSerialization()
    configureExceptions()
    authentication(Configuration.tokenValidationConfigAzure())
    routing {
        healthRoutes()
        authenticate("azure") {
            vedtakRoutes(vedtakService)
            behandlingRoutes(behandlingService)
            mottaRoutes(mottaNyttVedtakService)
        }
    }
}

fun Application.configureExceptions() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            ExceptionHandler.handle(call, cause)
        }
    }
}

fun Application.jacksonSerialization() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
        }
    }
}
