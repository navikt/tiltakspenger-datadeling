package no.nav.tiltakspenger.datadeling.routes.vedtak

import no.nav.tiltakspenger.datadeling.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.domene.BarnetilleggPeriode
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
    val rettighet: Rettighet,
    val periode: Periode,
    val kilde: Kilde,
    val barnetillegg: Barnetillegg?,
) {
    enum class Rettighet {
        TILTAKSPENGER,
        TILTAKSPENGER_OG_BARNETILLEGG,
    }

    data class Periode(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )
}

fun TiltakspengerVedtak.toVedtakDTO() =
    VedtakDTO(
        vedtakId = vedtakId,
        rettighet = VedtakDTO.Rettighet.valueOf(rettighet.name),
        periode = VedtakDTO.Periode(
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
        ),
        kilde = kilde,
        barnetillegg = barnetillegg,
    )

fun Vedtak.toVedtakDTO() =
    VedtakDTO(
        vedtakId = vedtakId,
        rettighet = VedtakDTO.Rettighet.valueOf(rettighet.name),
        periode = VedtakDTO.Periode(
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
        ),
        kilde = kilde,
        barnetillegg = if (rettighet == Rettighet.TILTAKSPENGER_OG_BARNETILLEGG && antallBarn > 0) {
            Barnetillegg(
                perioder = listOf(
                    BarnetilleggPeriode(
                        antallBarn = antallBarn,
                        periode = BarnetilleggPeriode.Periode(
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
