package no.nav.tiltakspenger.datadeling.motta.infra.http.server

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import java.time.Clock

fun Route.mottaRoutes(
    mottaNyttVedtakService: MottaNyttVedtakService,
    mottaNyBehanlingService: MottaNyBehandlingService,
    tokenService: TokenService,
    clock: Clock,
) {
    this.mottaNyttVedtakRoute(mottaNyttVedtakService, tokenService, clock)
    this.mottaNyBehandlingRoute(mottaNyBehanlingService, tokenService, clock)
}
