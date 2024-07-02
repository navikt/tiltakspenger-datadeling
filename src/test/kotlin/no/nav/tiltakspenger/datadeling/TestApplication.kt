package no.nav.tiltakspenger.datadeling

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.datadeling.service.VedtakService

fun ApplicationTestBuilder.configureTestApplication(
    vedtakService: VedtakService = mockk(),
) {
    application {
        jacksonSerialization()
        configureExceptions()
        authentication(testConfig())
        routing {
            healthRoutes()
            authenticate("azure") {
                vedtakRoutes(vedtakService)
            }
        }
    }
}

fun testConfig(
    wellknownUrl: String = "http://localhost:8080/azure/.well-known/openid-configuration",
    clientId: String = "validAudience",
) = TokenValidationConfig(
    name = "azure",
    discoveryUrl = wellknownUrl,
    acceptedAudience = listOf(clientId),
)
