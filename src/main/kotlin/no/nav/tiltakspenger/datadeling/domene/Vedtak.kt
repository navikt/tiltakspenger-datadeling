package no.nav.tiltakspenger.datadeling.domene

import java.time.LocalDate

data class Vedtak(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDager: Double,
    val dagsatsTiltakspenger: Int,
    val dagsatsBarnetillegg: Int,
    val antallBarn: Int,
    val relaterteTiltak: String,
    val rettighet: Rettighet,
    val vedtakId: String,
    val sakId: String,
    val saksnummer: String?,
    val kilde: String,
)

enum class Rettighet {
    TILTAKSPENGER,
    BARNETILLEGG,
    TILTAKSPENGER_OG_BARNETILLEGG,
    INGENTING,
}
