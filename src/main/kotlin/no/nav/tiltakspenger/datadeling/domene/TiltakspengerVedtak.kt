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
    override val periode: Periode,
    override val rettighet: Vedtak.Rettighet,
    val antallDagerPerMeldeperiode: Int,
    override val vedtakId: String,
    override val sakId: String,
    override val saksnummer: String,
    override val fnr: Fnr,
    val mottattTidspunkt: LocalDateTime = nå(),
    val opprettetTidspunkt: LocalDateTime,
) : Vedtak {
    // TODO post-mvp jah: Lag egen type for kilde.
    override val kilde = "tp"
}
