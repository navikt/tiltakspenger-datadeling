package no.nav.tiltakspenger.datadeling.testutils

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.application.configureExceptions
import no.nav.tiltakspenger.datadeling.application.jacksonSerialization
import no.nav.tiltakspenger.datadeling.application.setupAuthentication
import no.nav.tiltakspenger.datadeling.behandling.datadeling.BehandlingService
import no.nav.tiltakspenger.datadeling.behandling.datadeling.routes.behandlingRoutes
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.ArenaMeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.MeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes.arenaMeldekortRoutes
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes.meldekortRoutes
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.ArenaUtbetalingshistorikkService
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes.arenaUtbetalingshistorikkRoutes
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.VedtakService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import java.time.Instant

fun ApplicationTestBuilder.configureTestApplication(
    vedtakService: VedtakService = mockk(),
    meldekortService: MeldekortService = mockk(),
    behandlingService: BehandlingService = mockk(),
    arenaMeldekortService: ArenaMeldekortService = mockk(),
    arenaUtbetalingshistorikkService: ArenaUtbetalingshistorikkService = mockk(),
    texasClient: TexasClient,
) {
    application {
        jacksonSerialization()
        setupAuthentication(texasClient)
        configureExceptions()
        routing {
            healthRoutes()
            authenticate(IdentityProvider.AZUREAD.value) {
                arenaMeldekortRoutes(arenaMeldekortService)
                arenaUtbetalingshistorikkRoutes(arenaUtbetalingshistorikkService)
                vedtakRoutes(vedtakService)
                behandlingRoutes(behandlingService)
                meldekortRoutes(meldekortService)
            }
        }
    }
}
val token = AccessToken("token", Instant.now().plusSeconds(3600)) {}
