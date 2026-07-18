package no.nav.tiltakspenger.datadeling.vedtak

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

/**
 * Domenemodell for responsen fra /vedtak/tidslinje.
 */
data class VedtakTidslinje(
    val tidslinje: List<Vedtak>,
    val alleVedtak: List<Vedtak>,
    val vedtakFraArena: List<DatadelingsvedtakUtenAvslag>,
    val sak: VedtakSak?,
) {
    data class Vedtak(
        val vedtakId: String,
        val rettighet: Rettighet,
        val periode: Periode,
        val barnetillegg: Barnetillegg?,
        val vedtaksdato: LocalDate,
        val valgteHjemlerHarIkkeRettighet: List<ValgtHjemmelHarIkkeRettighet>?,
        val sats: Int?,
        val satsBarnetillegg: Int?,
        val vedtaksperiode: Periode,
        val innvilgelsesperioder: List<Periode>,
        val omgjortAvRammevedtakId: String?,
        val omgjorRammevedtakId: String?,
    ) {
        enum class Rettighet {
            TILTAKSPENGER,
            TILTAKSPENGER_OG_BARNETILLEGG,
            STANS,
            AVSLAG,
            OPPHOR,
        }

        data class Barnetillegg(
            val perioder: List<BarnetilleggPeriode>,
        ) {
            data class BarnetilleggPeriode(
                val antallBarn: Int,
                val periode: Periode,
            )
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
            IKKE_LOVLIG_OPPHOLD,
        }
    }
}

fun List<TiltakspengerVedtak>.toVedtakTidslinjeVedtak(log: KLogger, idag: LocalDate): List<VedtakTidslinje.Vedtak> {
    return this.map { it.toVedtakTidslinjeVedtak(log, idag) }
}

fun TiltakspengerVedtak.toVedtakTidslinjeVedtak(log: KLogger, idag: LocalDate): VedtakTidslinje.Vedtak {
    val satser = this.getSatser(log, idag)
    return VedtakTidslinje.Vedtak(
        vedtakId = this.vedtakId,
        rettighet = when (this.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> VedtakTidslinje.Vedtak.Rettighet.TILTAKSPENGER
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> VedtakTidslinje.Vedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
            TiltakspengerVedtak.Rettighet.STANS -> VedtakTidslinje.Vedtak.Rettighet.STANS
            TiltakspengerVedtak.Rettighet.AVSLAG -> VedtakTidslinje.Vedtak.Rettighet.AVSLAG
            TiltakspengerVedtak.Rettighet.OPPHØR -> VedtakTidslinje.Vedtak.Rettighet.OPPHOR
        },
        periode = when (this.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            -> this.innvilgelsesperiode!!

            TiltakspengerVedtak.Rettighet.STANS,
            TiltakspengerVedtak.Rettighet.AVSLAG,
            TiltakspengerVedtak.Rettighet.OPPHØR,
            -> this.virkningsperiode
        },
        barnetillegg = barnetillegg?.toVedtakTidslinjeBarnetillegg(),
        vedtaksdato = this.opprettet.toLocalDate(),
        valgteHjemlerHarIkkeRettighet = this.valgteHjemlerHarIkkeRettighet?.map {
            VedtakTidslinje.Vedtak.ValgtHjemmelHarIkkeRettighet.valueOf(it.name)
        },
        sats = satser?.sats,
        satsBarnetillegg = satser?.let {
            if (rettighet == TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG) {
                it.satsBarnetillegg
            } else {
                0
            }
        },
        vedtaksperiode = virkningsperiode,
        innvilgelsesperioder = innvilgelsesperiode?.let { listOf(it) } ?: emptyList(),
        omgjortAvRammevedtakId = this.omgjortAvRammevedtakId,
        omgjorRammevedtakId = this.omgjørRammevedtakId,
    )
}

private fun Barnetillegg.toVedtakTidslinjeBarnetillegg(): VedtakTidslinje.Vedtak.Barnetillegg {
    return VedtakTidslinje.Vedtak.Barnetillegg(
        perioder = this.perioder.map {
            VedtakTidslinje.Vedtak.Barnetillegg.BarnetilleggPeriode(
                antallBarn = it.antallBarn,
                periode = it.periode,
            )
        },
    )
}
