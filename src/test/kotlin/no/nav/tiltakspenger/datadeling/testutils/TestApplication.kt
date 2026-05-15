package no.nav.tiltakspenger.datadeling.testutils

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.infra.configureExceptions
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.meldekort.infra.MeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.infra.routes.meldekortRoutes
import no.nav.tiltakspenger.datadeling.vedtak.infra.VedtakService
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.vedtakRoutes
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
