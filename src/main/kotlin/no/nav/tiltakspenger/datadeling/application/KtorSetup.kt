package no.nav.tiltakspenger.datadeling.application

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.datadeling.CALL_ID_MDC_KEY
import no.nav.tiltakspenger.datadeling.Configuration
import no.nav.tiltakspenger.datadeling.Profile
import no.nav.tiltakspenger.datadeling.application.context.ApplicationContext
import no.nav.tiltakspenger.datadeling.application.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.behandling.datadeling.routes.behandlingRoutes
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes.arenaMeldekortRoutes
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes.meldekortRoutes
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.routes.mottaRoutes
import no.nav.tiltakspenger.datadeling.routes.swaggerRoute
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes.arenaUtbetalingshistorikkRoutes
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.TexasAuthenticationProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient

internal fun Application.ktorSetup(
    applicationContext: ApplicationContext,
) {
    install(CallId)
    install(CallLogging) {
        callIdMdc(CALL_ID_MDC_KEY)
        disableDefaultColors()
        filter { call ->
            !call.request.path().startsWith("/isalive") &&
                !call.request.path().startsWith("/isready") &&
                !call.request.path().startsWith("/metrics")
        }
    }
    jacksonSerialization()
    configureExceptions()
    setupAuthentication(applicationContext.texasClient)
    routing {
        healthRoutes()
        if (Configuration.applicationProfile() == Profile.DEV) {
            swaggerRoute()
        }
        authenticate(IdentityProvider.AZUREAD.value) {
            arenaMeldekortRoutes(applicationContext.arenaMeldekortService)
            arenaUtbetalingshistorikkRoutes(applicationContext.arenaUtbetalingshistorikkService)
            vedtakRoutes(applicationContext.vedtakService)
            behandlingRoutes(applicationContext.behandlingService)
            meldekortRoutes(applicationContext.meldekortService)
            mottaRoutes(
                applicationContext.mottaNyttVedtakService,
                applicationContext.mottaNyBehandlingService,
                applicationContext.clock,
                applicationContext.meldeperiodeRepo,
                applicationContext.godkjentMeldekortRepo,
                applicationContext.sakRepo,
            )
        }
    }
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
