package no.nav.tiltakspenger.datadeling.motta

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.behandling.motta.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.behandling.motta.routes.mottaNyBehandlingRoute
import no.nav.tiltakspenger.datadeling.vedtak.motta.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.motta.routes.mottaNyttVedtakRoute
import java.time.Clock

fun Route.mottaRoutes(
    mottaNyttVedtakService: MottaNyttVedtakService,
    mottaNyBehanlingService: MottaNyBehandlingService,
    clock: Clock,
) {
    this.mottaNyttVedtakRoute(mottaNyttVedtakService, clock)
    this.mottaNyBehandlingRoute(mottaNyBehanlingService, clock)
}
