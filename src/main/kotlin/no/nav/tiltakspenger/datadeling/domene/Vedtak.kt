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
    val kilde: String,
    val fnr: Fnr,
)

enum class Rettighet {
    TILTAKSPENGER,

    // TODO post-mvp jah: Man kan ikke få innvilget barnetillegg uten å ha fått innvilget tiltakspenger. Hvordan er denne tenkt brukt? Kan den komme fra Arena?
    BARNETILLEGG,
    TILTAKSPENGER_OG_BARNETILLEGG,
    INGENTING,
}
