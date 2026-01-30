package no.nav.tiltakspenger.datadeling.vedtak.domene

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.libs.satser.Satsdag
import no.nav.tiltakspenger.libs.satser.Satser
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domenemodell for tiltakspenger. Arena vil få sin egen modell.
 * På sikt vil den generelle Vedtak være et interface som dekker begge.
 *
 * Forhold mellom virkningsperiode og innvilgelsesperiode:
 * - Eksempel 1: Søknadsvedtak med innvilgelse fra 1. januar til 31. mars (virkningsperiode = innvilgelsesperiode). Dersom man omgjør dette vedtaket, vil den nye virkningsperioden _minimum_ være det samme som vedtaket man omgjør sin virkningsperiode, i dette tilfellet 1. januar til 31. mars. Innvilgelsperioden(e) kan derimot være kortere, f.eks. hele januar og hele mars. Da vil februar implisitt ikke lenger gi rett, og en potensiell feilutbetaling kan oppstå.
 * - Eksempel 2: Søknadsvedtak innvilges fra 3. januar til 28 januar. Vedtaket omgjøre og utvides til 1. til 31. januar. Virkningsperioden vil da være lik innvilgelsesperioden (1. til 31. januar).
 * - Eksempel 3: Søknadsvedtak innvilges fra 3. januar til 31 januar. Innvilgelsesperioden endres fra 1. til 28 januar. Virkningsperioden vil da være 1. til 31. januar. 29., 30. og 31. januar vil implisitt ikke lenger gi rett.
 * - Eksempel 4: Søknadsvedtak innvilges fra 1. januar til 28 januar. Innvilgelsesperioden endres fra 3. til 31 januar. Virkningsperioden vil da være 1. til 31. januar. 1. og 2. januar vil implisitt ikke lenger gi rett.
 * @param virkningsperiode Perioden vedtaket gjelder for (på tidspunktet det ble iverksatt). Ved søknadsbehandling/forlengelse/omgjøring vil den inneholde en eller flere innvilgelsesperioder. Alle vedtakstypene vil kun ha ett resultat, bortsett fra omgjøring som kan ha et kombinasjonsresultat av innvilgelse og opphør, hvor sistnevnte er implisitt.
 * @param innvilgelsesperiode null for avlag/stans/opphør. Vi har denne perioden i tillegg til virkningsperiode/vedtaksperiode for å støtte vedtak med kombinasjonsresultat; dvs. både innvilgelse og opphør i samme vedtaket. Dette vil skje i omgjøringsvedtak der innvilgelsesperioden er mindre enn virkningsperiode/vedtaksperioden, da vil den resterende perioden implisitt være opphørt.
 */
data class TiltakspengerVedtak(
    val virkningsperiode: Periode,
    val innvilgelsesperiode: Periode?,
    val omgjørRammevedtakId: String?,
    val omgjortAvRammevedtakId: String?,
    val rettighet: Rettighet,
    val vedtakId: String,
    val sakId: String,
    val mottattTidspunkt: LocalDateTime,
    override val opprettet: LocalDateTime,
    val barnetillegg: Barnetillegg?,
    val valgteHjemlerHarIkkeRettighet: List<ValgtHjemmelHarIkkeRettighet>?,
) : Periodiserbar {
    /** Reservert tidslinje-funksjonen. Bruk [virkningsperiode] eller [innvilgelsesperiode] istedenfor. */
    override val periode: Periode = virkningsperiode

    enum class Rettighet {
        TILTAKSPENGER,
        TILTAKSPENGER_OG_BARNETILLEGG,
        STANS,
        AVSLAG,
    }

    enum class ValgtHjemmelHarIkkeRettighet {
        DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK,
        ALDER,
        LIVSOPPHOLDSYTELSER,
        KVALIFISERINGSPROGRAMMET,
        INTRODUKSJONSPROGRAMMET,
        LONN_FRA_TILTAKSARRANGOR,
        LONN_FRA_ANDRE,
        INSTITUSJONSOPPHOLD,
        FREMMET_FOR_SENT,
    }

    /**
     * Tenkt brukt for å krympe vedtaket basert på tidslinja. Gir ikke mening og kalles for Avslag.
     * @return null dersom den nye virkningsperioden ikke overlapper med innvilgelsesperiode.
     * @throws IllegalArgumentException dersom nyVirkningsperiode ikke er innenfor gammel virkningsperiode.
     */
    fun krympVirkningsperiode(nyVirkningsperiode: Periode): TiltakspengerVedtak {
        require(this.virkningsperiode.inneholderHele(nyVirkningsperiode)) {
            "Kan kun krympe virkningsperiode. Ny virkningsperiode $nyVirkningsperiode er ikke innenfor gammel virkningsperiode ${this.virkningsperiode} for vedtak med id $vedtakId"
        }
        return when (this.rettighet) {
            Rettighet.AVSLAG, Rettighet.STANS -> this.copy(
                virkningsperiode = nyVirkningsperiode,
                barnetillegg = barnetillegg?.oppdaterPeriode(nyVirkningsperiode),
            )

            Rettighet.TILTAKSPENGER, Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> {
                val nyInnvilgelsesperiode = this.innvilgelsesperiode?.let { nyVirkningsperiode.overlappendePeriode(it) }
                this.copy(
                    virkningsperiode = nyVirkningsperiode,
                    innvilgelsesperiode = nyInnvilgelsesperiode,
                    barnetillegg = barnetillegg?.oppdaterPeriode(nyInnvilgelsesperiode ?: virkningsperiode),
                )
            }
        }
    }

    fun getSatser(log: KLogger, idag: LocalDate = LocalDate.now()): Satsdag? {
        if (rettighet == Rettighet.STANS || rettighet == Rettighet.AVSLAG) {
            return null
        }

        val dato = if (idag.isBefore(innvilgelsesperiode!!.fraOgMed)) {
            innvilgelsesperiode.fraOgMed
        } else if (idag.isAfter(innvilgelsesperiode.tilOgMed)) {
            innvilgelsesperiode.tilOgMed
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
