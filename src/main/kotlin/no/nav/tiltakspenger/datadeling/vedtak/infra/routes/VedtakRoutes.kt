package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.vedtak.HentSakService
import no.nav.tiltakspenger.datadeling.vedtak.HentTidslinjeOgAlleVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.HentTpVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.HentVedtaksperioderService

const val VEDTAK_PATH = "/vedtak"

fun Route.vedtakRoutes(
    hentTpVedtakService: HentTpVedtakService,
    hentTidslinjeOgAlleVedtakService: HentTidslinjeOgAlleVedtakService,
    hentVedtaksperioderService: HentVedtaksperioderService,
    hentSakService: HentSakService,
) {
    hentVedtakDetaljerRoute(hentTpVedtakService)
    hentVedtakTidslinjeRoute(hentTidslinjeOgAlleVedtakService)
    hentVedtakPerioderRoute(hentVedtaksperioderService)
    hentSakRoute(hentSakService)
}
