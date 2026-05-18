package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.vedtak.infra.HentSakService
import no.nav.tiltakspenger.datadeling.vedtak.infra.HentTidslinjeOgAlleVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.infra.HentTpVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.infra.HentVedtaksperioderService

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
