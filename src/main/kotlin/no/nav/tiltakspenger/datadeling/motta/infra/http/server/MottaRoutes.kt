package no.nav.tiltakspenger.datadeling.motta.infra.http.server

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakService
import no.nav.tiltakspenger.libs.auth.core.TokenService

fun Route.mottaRoutes(
    mottaNyttVedtakService: MottaNyttVedtakService,
    tokenService: TokenService,
) {
    this.mottaNyttVedtakRoute(mottaNyttVedtakService, tokenService)
}
