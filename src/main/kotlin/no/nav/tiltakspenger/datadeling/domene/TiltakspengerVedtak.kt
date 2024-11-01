package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDateTime

data class TiltakspengerVedtak(
    val periode: Periode,
    val antallDagerPerMeldeperiode: Int,
    val meldeperiodensLengde: Int,
    val dagsatsTiltakspenger: Int,
    val dagsatsBarnetillegg: Int,
    val antallBarn: Int,
    val tiltaksgjennomføringId: String,
    val rettighet: Rettighet,
    val vedtakId: String,
    val sakId: String,
    val saksnummer: String?,
    // TODO post-mvp jah: Lag egen type for kilde.
    val kilde: String,
    val fnr: Fnr,
    val mottattTidspunkt: LocalDateTime,
    val opprettetTidspunkt: LocalDateTime,
) {
    enum class Rettighet {
        TILTAKSPENGER,
        // TODO post-mvp jah: Legg til støtte for barnetillegg og avslag når vi får det i saksbehandling-api
    }
}
