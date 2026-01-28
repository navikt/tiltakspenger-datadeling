package no.nav.tiltakspenger.datadeling.client.arena.domene

data class ArenaUtbetalingshistorikkDetaljer(
    val vedtakfakta: ArenaVedtakfakta?,
    val anmerkninger: List<ArenaAnmerkning>,
)
