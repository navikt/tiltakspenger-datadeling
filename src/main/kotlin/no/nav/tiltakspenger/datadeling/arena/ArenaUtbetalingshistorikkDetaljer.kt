package no.nav.tiltakspenger.datadeling.arena

data class ArenaUtbetalingshistorikkDetaljer(
    val vedtakfakta: ArenaVedtakfakta?,
    val anmerkninger: List<ArenaAnmerkning>,
)
