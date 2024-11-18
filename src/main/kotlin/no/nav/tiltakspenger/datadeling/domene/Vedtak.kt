package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

sealed interface Vedtak {
    val periode: Periode
    val rettighet: Rettighet
    val vedtakId: String
    val sakId: String
    val saksnummer: String?
    val kilde: String
    val fnr: Fnr

    enum class Rettighet {
        TILTAKSPENGER,

        // TODO post-mvp jah: Man kan ikke få innvilget barnetillegg uten å ha fått innvilget tiltakspenger. Hvordan er denne tenkt brukt? Kan den komme fra Arena?
        BARNETILLEGG,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }
}
