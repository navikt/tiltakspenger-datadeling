package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.vedtak.infra.VedtakService

const val VEDTAK_PATH = "/vedtak"

fun Route.vedtakRoutes(
    vedtakService: VedtakService,
) {
    hentVedtakDetaljerRoute(vedtakService)
    hentVedtakTidslinjeRoute(vedtakService)
    hentVedtakPerioderRoute(vedtakService)
    hentSakRoute(vedtakService)
}
