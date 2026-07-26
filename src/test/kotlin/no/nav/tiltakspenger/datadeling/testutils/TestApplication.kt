package no.nav.tiltakspenger.datadeling.testutils

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.infra.configureExceptions
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.meldekort.HentMeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.infra.routes.hentMeldekortDetaljerRoute
import no.nav.tiltakspenger.datadeling.vedtak.HentSakService
import no.nav.tiltakspenger.datadeling.vedtak.HentVedtakDetaljerService
import no.nav.tiltakspenger.datadeling.vedtak.HentVedtakPerioderService
import no.nav.tiltakspenger.datadeling.vedtak.HentVedtakTidslinjeService
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.hentSakRoute
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.hentVedtakDetaljerRoute
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.hentVedtakPerioderRoute
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.hentVedtakTidslinjeRoute
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.ktor.common.oppstart.healthRoutes
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import java.time.Clock
import java.time.Instant

/**
 * Monterer leseendepunktene enkeltvis i stedet for via feature-modulene.
 * Da kan hver test sende inn akkurat den servicen den bryr seg om, og la resten være mocks.
 */
fun ApplicationTestBuilder.configureTestApplication(
    hentVedtakDetaljerService: HentVedtakDetaljerService = mockk(),
    hentVedtakTidslinjeService: HentVedtakTidslinjeService = mockk(),
    hentVedtakPerioderService: HentVedtakPerioderService = mockk(),
    hentSakService: HentSakService = mockk(),
    hentMeldekortService: HentMeldekortService = mockk(),
    texasClient: TexasClient,
    clock: Clock = fixedClock,
) {
    application {
        jacksonSerialization()
        setupAuthentication(texasClient)
        configureExceptions()
        routing {
            healthRoutes { true }
            authenticate(IdentityProvider.AZUREAD.value) {
                hentVedtakDetaljerRoute(hentVedtakDetaljerService, clock)
                hentVedtakTidslinjeRoute(hentVedtakTidslinjeService)
                hentVedtakPerioderRoute(hentVedtakPerioderService)
                hentSakRoute(hentSakService)
                hentMeldekortDetaljerRoute(hentMeldekortService)
            }
        }
    }
}
val token = AccessToken("token", Instant.now(fixedClock).plusSeconds(3600))
