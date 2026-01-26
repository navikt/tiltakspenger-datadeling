package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes

data class ArenaUtbetalingshistorikkDetaljerResponse(
    val vedtakfakta: ArenaVedtakfaktaResponse,
    val anmerkninger: List<ArenaAnmerkningResponse>,
)
