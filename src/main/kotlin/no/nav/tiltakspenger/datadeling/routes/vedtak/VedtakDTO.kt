package no.nav.tiltakspenger.datadeling.routes.vedtak

import no.nav.tiltakspenger.datadeling.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

/**
 * Kontrakt for vedtaksperioder. Brukes av modia-personoversikt.
 */
data class VedtakDTO(
    val vedtakId: String,
    val rettighet: RettighetDTO,
    val periode: PeriodeDTO,
    val kilde: KildeDTO,
    val barnetillegg: BarnetilleggDTO?,
) {
    enum class RettighetDTO {
        TILTAKSPENGER,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }

    enum class KildeDTO {
        TPSAK,
        ARENA,
    }

    data class BarnetilleggDTO(
        val perioder: List<BarnetilleggPeriodeDTO>,
    ) {
        data class BarnetilleggPeriodeDTO(
            val antallBarn: Int,
            val periode: PeriodeDTO,
        )
    }

    /** Siden dette er et ekstern API ønsker vi ikke å bruke libs her. */
    data class PeriodeDTO(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )
}

fun Barnetillegg.toDTO(): VedtakDTO.BarnetilleggDTO {
    return VedtakDTO.BarnetilleggDTO(
        perioder = this.perioder.map {
            VedtakDTO.BarnetilleggDTO.BarnetilleggPeriodeDTO(
                antallBarn = it.antallBarn,
                periode = VedtakDTO.PeriodeDTO(
                    fraOgMed = it.periode.fraOgMed,
                    tilOgMed = it.periode.tilOgMed,
                ),
            )
        },
    )
}

fun TiltakspengerVedtak.toVedtakDTO() =
    VedtakDTO(
        vedtakId = vedtakId,
        rettighet = VedtakDTO.RettighetDTO.valueOf(rettighet.name),
        periode = VedtakDTO.PeriodeDTO(
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
        ),
        kilde = when (kilde) {
            Kilde.TPSAK -> VedtakDTO.KildeDTO.TPSAK
            Kilde.ARENA -> VedtakDTO.KildeDTO.ARENA
        },
        barnetillegg = barnetillegg?.toDTO(),
    )

fun Vedtak.toVedtakDTO() =
    VedtakDTO(
        vedtakId = vedtakId,
        rettighet = VedtakDTO.RettighetDTO.valueOf(rettighet.name),
        periode = VedtakDTO.PeriodeDTO(
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
        ),
        kilde = when (kilde) {
            Kilde.TPSAK -> VedtakDTO.KildeDTO.TPSAK
            Kilde.ARENA -> VedtakDTO.KildeDTO.ARENA
        },
        barnetillegg = if (rettighet == Rettighet.TILTAKSPENGER_OG_BARNETILLEGG && antallBarn > 0) {
            VedtakDTO.BarnetilleggDTO(
                perioder = listOf(
                    VedtakDTO.BarnetilleggDTO.BarnetilleggPeriodeDTO(
                        antallBarn = antallBarn,
                        periode = VedtakDTO.PeriodeDTO(
                            fraOgMed = periode.fraOgMed,
                            tilOgMed = periode.tilOgMed,
                        ),
                    ),
                ),
            )
        } else {
            null
        },
    )
