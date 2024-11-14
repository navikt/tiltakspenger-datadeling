package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDateTime

/**
 * Domenemodell for tiltakspenger. Arena vil få sin egen modell.
 * På sikt vil den generelle Vedtak være et interface som dekker begge.
 */
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
    val fnr: Fnr,
    val mottattTidspunkt: LocalDateTime = nå(),
    val opprettetTidspunkt: LocalDateTime,
) {
    // TODO post-mvp jah: Lag egen type for kilde.
    val kilde = "tp"

    enum class Rettighet {
        TILTAKSPENGER,
        // TODO post-mvp jah: Legg til støtte for barnetillegg og avslag når vi får det i saksbehandling-api
    }
}
