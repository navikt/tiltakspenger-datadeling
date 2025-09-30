package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

// TODO post-mvp jah: Jeg har opprettet TiltakspengerVedtak. Vi kan gjøre om dette til Arena sitt og lage et felles interface.
/**
 * @param tiltaksgjennomføringId Knytningen mellom en person (fnr) som gjennomfører et gitt tiltak (tiltakId).
 */
data class Vedtak(
    val periode: Periode,
    val rettighet: Rettighet,
    val vedtakId: String,
    val sakId: String,
    val saksnummer: String?,
    val kilde: Kilde,
    val fnr: Fnr,
    val antallBarn: Int,
    val dagsatsTiltakspenger: Int?,
    val dagsatsBarnetillegg: Int?,
)

enum class Rettighet {
    TILTAKSPENGER,

    // I en periode kunne man få innvilget kun barnetillegg i Arena, typisk i kombinasjon med andre ytelser som ikke hadde barnetillegg
    BARNETILLEGG,
    TILTAKSPENGER_OG_BARNETILLEGG,
    INGENTING,
}
