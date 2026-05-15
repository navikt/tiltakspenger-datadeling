package no.nav.tiltakspenger.datadeling.arena

import no.nav.tiltakspenger.datadeling.Kilde
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

data class ArenaVedtak(
    val periode: Periode,
    val rettighet: Rettighet,
    val vedtakId: String,
    val kilde: Kilde,
    val fnr: Fnr,
    val antallBarn: Int,
    val dagsatsTiltakspenger: Int?,
    val dagsatsBarnetillegg: Int?,
    val beslutningsdato: LocalDate?,
    val sak: Sak,
) {
    data class Sak(
        val sakId: String,
        val saksnummer: String,
        val opprettetDato: LocalDate,
        val status: String,
    )
}

enum class Rettighet {
    TILTAKSPENGER,

    // I en periode kunne man få innvilget kun barnetillegg i Arena, typisk i kombinasjon med andre ytelser som ikke hadde barnetillegg
    BARNETILLEGG,
    TILTAKSPENGER_OG_BARNETILLEGG,
    INGENTING,
}
