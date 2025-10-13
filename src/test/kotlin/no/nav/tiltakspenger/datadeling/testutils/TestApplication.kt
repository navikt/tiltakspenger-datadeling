package no.nav.tiltakspenger.datadeling.testutils

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.application.configureExceptions
import no.nav.tiltakspenger.datadeling.application.jacksonSerialization
import no.nav.tiltakspenger.datadeling.application.setupAuthentication
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.MeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes.meldekortRoutes
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.VedtakService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import java.time.Instant

fun ApplicationTestBuilder.configureTestApplication(
    vedtakService: VedtakService = mockk(),
    meldekortService: MeldekortService = mockk(),
    texasClient: TexasClient,
) {
    application {
        jacksonSerialization()
        setupAuthentication(texasClient)
        configureExceptions()
        routing {
            healthRoutes()
            authenticate(IdentityProvider.AZUREAD.value) {
                vedtakRoutes(vedtakService)
                meldekortRoutes(meldekortService)
            }
        }
    }
}
val token = AccessToken("token", Instant.now().plusSeconds(3600)) {}
