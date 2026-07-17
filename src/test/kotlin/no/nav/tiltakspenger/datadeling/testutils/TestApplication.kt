package no.nav.tiltakspenger.datadeling.testutils

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.infra.configureExceptions
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.meldekort.infra.MeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.infra.routes.meldekortRoutes
import no.nav.tiltakspenger.datadeling.vedtak.HentSakService
import no.nav.tiltakspenger.datadeling.vedtak.HentTidslinjeOgAlleVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.HentTpVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.HentVedtaksperioderService
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.vedtakRoutes
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.ktor.common.oppstart.healthRoutes
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import java.time.Instant

fun ApplicationTestBuilder.configureTestApplication(
    hentTpVedtakService: HentTpVedtakService = mockk(),
    hentTidslinjeOgAlleVedtakService: HentTidslinjeOgAlleVedtakService = mockk(),
    hentVedtaksperioderService: HentVedtaksperioderService = mockk(),
    hentSakService: HentSakService = mockk(),
    meldekortService: MeldekortService = mockk(),
    texasClient: TexasClient,
) {
    application {
        jacksonSerialization()
        setupAuthentication(texasClient)
        configureExceptions()
        routing {
            healthRoutes { true }
            authenticate(IdentityProvider.AZUREAD.value) {
                vedtakRoutes(
                    hentTpVedtakService = hentTpVedtakService,
                    hentTidslinjeOgAlleVedtakService = hentTidslinjeOgAlleVedtakService,
                    hentVedtaksperioderService = hentVedtaksperioderService,
                    hentSakService = hentSakService,
                )
                meldekortRoutes(meldekortService)
            }
        }
    }
}
val token = AccessToken("token", Instant.now().plusSeconds(3600))
