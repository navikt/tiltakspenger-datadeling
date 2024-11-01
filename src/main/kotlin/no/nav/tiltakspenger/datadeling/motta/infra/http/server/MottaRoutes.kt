package no.nav.tiltakspenger.datadeling.motta.infra.http.server

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakService

fun Route.mottaRoutes(
    mottaNyttVedtakService: MottaNyttVedtakService,
) {
    this.mottaNyttVedtakRoute(mottaNyttVedtakService)
}
