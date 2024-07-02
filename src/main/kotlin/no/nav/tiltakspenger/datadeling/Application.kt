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
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.Configuration.httpPort
import no.nav.tiltakspenger.datadeling.auth.AzureTokenProvider
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClientImpl
import no.nav.tiltakspenger.datadeling.client.vedtak.VedtakClientImpl
import no.nav.tiltakspenger.datadeling.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.datadeling.service.VedtakServiceImpl

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    embeddedServer(Netty, port = httpPort(), module = Application::module).start(wait = true)
}

fun Application.module() {
    val tokenProviderVedtak = AzureTokenProvider(config = Configuration.oauthConfigVedtak())
    val tokenProviderArena = AzureTokenProvider(config = Configuration.oauthConfigArena())

    val vedtakClient = VedtakClientImpl(getToken = tokenProviderVedtak::getToken)
    val arenaClient = ArenaClientImpl(getToken = tokenProviderArena::getToken)

    val vedtakService = VedtakServiceImpl(vedtakClient, arenaClient)

    jacksonSerialization()
    configureExceptions()
    authentication(Configuration.tokenValidationConfigAzure())
    routing {
        healthRoutes()
        authenticate("azure") {
            vedtakRoutes(vedtakService)
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
