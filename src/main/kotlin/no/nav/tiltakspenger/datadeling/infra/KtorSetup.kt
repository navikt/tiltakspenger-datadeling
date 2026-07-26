package no.nav.tiltakspenger.datadeling.infra

import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.datadeling.arena.infra.routes.arenaModule
import no.nav.tiltakspenger.datadeling.behandling.infra.routes.behandlingModule
import no.nav.tiltakspenger.datadeling.infra.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.infra.routes.swaggerRoute
import no.nav.tiltakspenger.datadeling.meldekort.infra.routes.meldekortModule
import no.nav.tiltakspenger.datadeling.sak.infra.routes.sakModule
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.vedtakModule
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Readiness
import no.nav.tiltakspenger.libs.ktor.common.oppstart.healthRoutes
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.TexasAuthenticationProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient

/**
 * Generisk Ktor-oppsett: plugins, autentisering og oppkobling av modulene per feature.
 *
 * Hver feature-modul eier sin egen auth-provider og sine egne routes, via `*Module(applicationContext)`-funksjonene.
 * Alle modulene bruker i dag [no.nav.tiltakspenger.libs.texas.IdentityProvider.AZUREAD]; det som skiller endepunktene er hvilken systembrukerrolle de krever.
 *
 * [visSwagger] sendes inn fra oppstarten framfor å leses fra [Configuration] her, slik at oppsettet kan settes opp fra test uten å mutere global system-env.
 */
internal fun Application.ktorSetup(
    applicationContext: ApplicationContext,
    readiness: Readiness,
    visSwagger: Boolean,
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
        healthRoutes { readiness.erKlar() }
        if (visSwagger) {
            swaggerRoute()
        }

        sakModule(applicationContext)
        vedtakModule(applicationContext)
        behandlingModule(applicationContext)
        meldekortModule(applicationContext)
        arenaModule(applicationContext)
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
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
}
