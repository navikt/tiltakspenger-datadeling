package no.nav.tiltakspenger.datadeling.motta

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.motta.behandling.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.motta.behandling.route.mottaNyBehandlingRoute
import no.nav.tiltakspenger.datadeling.motta.vedtak.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.motta.vedtak.route.mottaNyttVedtakRoute
import java.time.Clock

fun Route.mottaRoutes(
    mottaNyttVedtakService: MottaNyttVedtakService,
    mottaNyBehanlingService: MottaNyBehandlingService,
    clock: Clock,
) {
    this.mottaNyttVedtakRoute(mottaNyttVedtakService, clock)
    this.mottaNyBehandlingRoute(mottaNyBehanlingService, clock)
}
