package no.nav.tiltakspenger.datadeling.motta.infra.http.server

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakService
import java.time.Clock

fun Route.mottaRoutes(
    mottaNyttVedtakService: MottaNyttVedtakService,
    mottaNyBehanlingService: MottaNyBehandlingService,
    clock: Clock,
) {
    this.mottaNyttVedtakRoute(mottaNyttVedtakService, clock)
    this.mottaNyBehandlingRoute(mottaNyBehanlingService, clock)
}
