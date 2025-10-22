package no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import java.time.LocalDate

data class VedtakTidslinjeResponse(
    val tidslinje: List<VedtakResponse>,
    val alleVedtak: List<VedtakResponse>,
) {
    data class VedtakResponse(
        val vedtakId: String,
        val sakId: String,
        val saksnummer: String,
        val rettighet: RettighetDTO,
        val periode: PeriodeDTO,
        val barnetillegg: BarnetilleggDTO?,
        val vedtaksdato: LocalDate,
        val valgteHjemlerHarIkkeRettighet: List<ValgtHjemmelHarIkkeRettighetDTO>?,
        val sats: Int?,
        val satsBarnetillegg: Int?,
        val vedtaksperiode: VedtakDTO.PeriodeDTO,
        val innvilgelsesperioder: List<VedtakDTO.PeriodeDTO>,
        val omgjortAvRammevedtakId: String?,
        val omgjorRammevedtakId: String?,
    ) {
        enum class RettighetDTO {
            TILTAKSPENGER,
            TILTAKSPENGER_OG_BARNETILLEGG,
            STANS,
            AVSLAG,
        }

        data class BarnetilleggDTO(
            val perioder: List<BarnetilleggPeriodeDTO>,
        ) {
            data class BarnetilleggPeriodeDTO(
                val antallBarn: Int,
                val periode: PeriodeDTO,
            )
        }

        data class PeriodeDTO(
            val fraOgMed: LocalDate,
            val tilOgMed: LocalDate,
        )

        enum class ValgtHjemmelHarIkkeRettighetDTO {
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
    }
}

fun List<TiltakspengerVedtak>.toVedtakResponse(log: KLogger): List<VedtakTidslinjeResponse.VedtakResponse> {
    return this.map { it.toVedtakResponse(log) }
}

fun TiltakspengerVedtak.toVedtakResponse(log: KLogger): VedtakTidslinjeResponse.VedtakResponse {
    val satser = this.getSatser(log)
    return VedtakTidslinjeResponse.VedtakResponse(
        vedtakId = this.vedtakId,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        rettighet = when (this.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> VedtakTidslinjeResponse.VedtakResponse.RettighetDTO.TILTAKSPENGER
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> VedtakTidslinjeResponse.VedtakResponse.RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG
            TiltakspengerVedtak.Rettighet.STANS -> VedtakTidslinjeResponse.VedtakResponse.RettighetDTO.STANS
            TiltakspengerVedtak.Rettighet.AVSLAG -> VedtakTidslinjeResponse.VedtakResponse.RettighetDTO.AVSLAG
        },
        periode = when (this.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            -> this.innvilgelsesperiode!!

            TiltakspengerVedtak.Rettighet.STANS,
            TiltakspengerVedtak.Rettighet.AVSLAG,
            -> this.virkningsperiode
        }.let {
            VedtakTidslinjeResponse.VedtakResponse.PeriodeDTO(
                fraOgMed = it.fraOgMed,
                tilOgMed = it.tilOgMed,
            )
        },
        barnetillegg = barnetillegg?.toVedtakResponseDTO(),
        vedtaksdato = this.opprettet.toLocalDate(),
        valgteHjemlerHarIkkeRettighet = this.valgteHjemlerHarIkkeRettighet?.map {
            VedtakTidslinjeResponse.VedtakResponse.ValgtHjemmelHarIkkeRettighetDTO.valueOf(
                it.name,
            )
        },
        sats = satser?.sats,
        satsBarnetillegg = satser?.let {
            if (rettighet == TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG) {
                it.satsBarnetillegg
            } else {
                0
            }
        },
        vedtaksperiode = virkningsperiode.let { VedtakDTO.PeriodeDTO(it.fraOgMed, it.tilOgMed) },
        innvilgelsesperioder = innvilgelsesperiode?.let {
            listOf(VedtakDTO.PeriodeDTO(it.fraOgMed, it.tilOgMed))
        } ?: emptyList(),
        omgjortAvRammevedtakId = this.omgjortAvRammevedtakId,
        omgjorRammevedtakId = this.omgjørRammevedtakId,
    )
}

private fun Barnetillegg.toVedtakResponseDTO(): VedtakTidslinjeResponse.VedtakResponse.BarnetilleggDTO {
    return VedtakTidslinjeResponse.VedtakResponse.BarnetilleggDTO(
        perioder = this.perioder.map {
            VedtakTidslinjeResponse.VedtakResponse.BarnetilleggDTO.BarnetilleggPeriodeDTO(
                antallBarn = it.antallBarn,
                periode = VedtakTidslinjeResponse.VedtakResponse.PeriodeDTO(
                    fraOgMed = it.periode.fraOgMed,
                    tilOgMed = it.periode.tilOgMed,
                ),
            )
        },
    )
}
