package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import java.time.LocalDateTime

/**
 * Domenemodell for tiltakspenger. Arena vil få sin egen modell.
 * På sikt vil den generelle Vedtak være et interface som dekker begge.
 */
data class TiltakspengerVedtak(
    override val periode: Periode,
    val rettighet: Rettighet,
    val antallDagerPerMeldeperiode: Int,
    val vedtakId: String,
    val sakId: String,
    val saksnummer: String,
    val fnr: Fnr,
    val mottattTidspunkt: LocalDateTime,
    override val opprettet: LocalDateTime,
    val barnetillegg: Barnetillegg?,
) : Periodiserbar {
    val kilde = Kilde.TPSAK

    enum class Rettighet {
        TILTAKSPENGER,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
        // TODO post-mvp jah: Legg til støtte for avslag når vi får det i saksbehandling-api
    }

    fun oppdaterPeriode(nyPeriode: Periode): TiltakspengerVedtak {
        return this.copy(
            periode = nyPeriode,
            barnetillegg = barnetillegg?.oppdaterPeriode(nyPeriode),
        )
    }
}
