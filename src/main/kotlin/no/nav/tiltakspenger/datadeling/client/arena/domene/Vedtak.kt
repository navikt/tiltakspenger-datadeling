package no.nav.tiltakspenger.datadeling.client.arena.domene

import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.sak.dto.SakDTO
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

// TODO post-mvp jah: Jeg har opprettet TiltakspengerVedtak. Vi kan gjøre om dette til Arena sitt og lage et felles interface.
data class Vedtak(
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
    ) {
        fun toSakDTO() = SakDTO(
            sakId = sakId,
            saksnummer = saksnummer,
            opprettetDato = opprettetDato.atTime(9, 0),
            status = status,
            kilde = "ARENA",
        )
    }
}

enum class Rettighet {
    TILTAKSPENGER,

    // I en periode kunne man få innvilget kun barnetillegg i Arena, typisk i kombinasjon med andre ytelser som ikke hadde barnetillegg
    BARNETILLEGG,
    TILTAKSPENGER_OG_BARNETILLEGG,
    INGENTING,
}
