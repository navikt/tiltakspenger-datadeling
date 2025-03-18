package no.nav.tiltakspenger.datadeling.routes

import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.configureExceptions
import no.nav.tiltakspenger.datadeling.jacksonSerialization
import no.nav.tiltakspenger.datadeling.routes.vedtak.vedtakRoutes
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.test.core.tokenServiceForTest
import no.nav.tiltakspenger.libs.common.AccessToken
import java.time.Instant

fun ApplicationTestBuilder.configureTestApplication(
    vedtakService: VedtakService = mockk(),
    tokenService: TokenService = tokenServiceForTest(),
) {
    application {
        jacksonSerialization()
        configureExceptions()
        routing {
            healthRoutes()
            vedtakRoutes(vedtakService, tokenService)
        }
    }
}
val token = AccessToken("token", Instant.now().plusSeconds(3600)) {}
