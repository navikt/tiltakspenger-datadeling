package no.nav.tiltakspenger.datadeling.domene.dto

data class Sak(
    val sakId: String,
    val saksnummer: String,
    val kilde: String = "TPSAK",
    val status: String = "Løpende",
)
