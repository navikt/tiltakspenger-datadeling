package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import no.nav.tiltakspenger.datadeling.vedtak.VedtakSak
import no.nav.tiltakspenger.datadeling.vedtak.VedtakTidslinje
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class VedtakTidslinjeResponse(
    val tidslinje: List<VedtakResponse>,
    val alleVedtak: List<VedtakResponse>,
    val vedtakFraArena: List<VedtakDTO>,
    val sak: VedtakTidslinjeSakDTO?,
) {
    data class VedtakResponse(
        val vedtakId: String,
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
            OPPHOR,
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
            IKKE_LOVLIG_OPPHOLD,
        }
    }
}

data class VedtakTidslinjeSakDTO(
    val sakId: String,
    val saksnummer: String,
    val kilde: String,
    val status: String,
    val opprettetDato: LocalDateTime,
)

fun VedtakTidslinje.toVedtakTidslinjeResponse(): VedtakTidslinjeResponse {
    return VedtakTidslinjeResponse(
        tidslinje = tidslinje.map { it.toVedtakResponse() },
        alleVedtak = alleVedtak.map { it.toVedtakResponse() },
        vedtakFraArena = vedtakFraArena.map { it.toVedtakDTO() },
        sak = sak?.toDTO(),
    )
}

private fun VedtakTidslinje.Vedtak.toVedtakResponse(): VedtakTidslinjeResponse.VedtakResponse {
    return VedtakTidslinjeResponse.VedtakResponse(
        vedtakId = vedtakId,
        rettighet = VedtakTidslinjeResponse.VedtakResponse.RettighetDTO.valueOf(rettighet.name),
        periode = periode.toVedtakResponsePeriodeDTO(),
        barnetillegg = barnetillegg?.toDTO(),
        vedtaksdato = vedtaksdato,
        valgteHjemlerHarIkkeRettighet = valgteHjemlerHarIkkeRettighet?.map {
            VedtakTidslinjeResponse.VedtakResponse.ValgtHjemmelHarIkkeRettighetDTO.valueOf(
                it.name,
            )
        },
        sats = sats,
        satsBarnetillegg = satsBarnetillegg,
        vedtaksperiode = vedtaksperiode.toVedtakDTOPeriodeDTO(),
        innvilgelsesperioder = innvilgelsesperioder.map { it.toVedtakDTOPeriodeDTO() },
        omgjortAvRammevedtakId = omgjortAvRammevedtakId,
        omgjorRammevedtakId = omgjorRammevedtakId,
    )
}

private fun VedtakTidslinje.Vedtak.Barnetillegg.toDTO(): VedtakTidslinjeResponse.VedtakResponse.BarnetilleggDTO {
    return VedtakTidslinjeResponse.VedtakResponse.BarnetilleggDTO(
        perioder = perioder.map {
            VedtakTidslinjeResponse.VedtakResponse.BarnetilleggDTO.BarnetilleggPeriodeDTO(
                antallBarn = it.antallBarn,
                periode = it.periode.toVedtakResponsePeriodeDTO(),
            )
        },
    )
}

private fun Periode.toVedtakResponsePeriodeDTO(): VedtakTidslinjeResponse.VedtakResponse.PeriodeDTO =
    VedtakTidslinjeResponse.VedtakResponse.PeriodeDTO(
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
    )

private fun Periode.toVedtakDTOPeriodeDTO(): VedtakDTO.PeriodeDTO =
    VedtakDTO.PeriodeDTO(
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
    )

private fun VedtakSak.toDTO(): VedtakTidslinjeSakDTO =
    VedtakTidslinjeSakDTO(
        sakId = sakId,
        saksnummer = saksnummer,
        kilde = kilde,
        status = status,
        opprettetDato = opprettetDato,
    )
