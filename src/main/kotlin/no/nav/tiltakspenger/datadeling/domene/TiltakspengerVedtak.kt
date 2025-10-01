package no.nav.tiltakspenger.datadeling.domene

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.libs.satser.Satsdag
import no.nav.tiltakspenger.libs.satser.Satser
import java.time.LocalDate
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
        STANS,
        AVSLAG,
    }

    fun oppdaterPeriode(nyPeriode: Periode): TiltakspengerVedtak {
        return this.copy(
            periode = nyPeriode,
            barnetillegg = barnetillegg?.oppdaterPeriode(nyPeriode),
        )
    }

    fun getSatser(log: KLogger, idag: LocalDate = LocalDate.now()): Satsdag? {
        if (rettighet == Rettighet.STANS || rettighet == Rettighet.AVSLAG) {
            return null
        }

        val dato = if (idag.isBefore(periode.fraOgMed)) {
            periode.fraOgMed
        } else if (idag.isAfter(periode.tilOgMed)) {
            periode.tilOgMed
        } else {
            idag
        }
        try {
            return Satser.sats(dato)
        } catch (e: Exception) {
            log.warn { "Fant ikke sats for vedtak med id $vedtakId: ${e.message}" }
        }
        return null
    }
}
