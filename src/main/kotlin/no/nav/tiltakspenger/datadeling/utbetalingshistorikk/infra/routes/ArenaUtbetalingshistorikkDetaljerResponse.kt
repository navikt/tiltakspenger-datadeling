package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.routes

data class ArenaUtbetalingshistorikkDetaljerResponse(
    val vedtakfakta: ArenaVedtakfaktaResponse?,
    val anmerkninger: List<ArenaAnmerkningResponse>,
)
