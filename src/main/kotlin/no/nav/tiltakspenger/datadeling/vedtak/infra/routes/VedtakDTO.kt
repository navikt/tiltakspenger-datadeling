package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import no.nav.tiltakspenger.datadeling.vedtak.DatadelingsvedtakUtenAvslag
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Kontrakt for vedtaksperioder.
 * Brukes av modia-personoversikt.
 * @param periode deprecated - bruk vedtaksperiode + innvilgelsesperioder
 * @param vedtaksperiode kan være større enn innvilgelsesperioden ved omgjøring.
 * Kalles virkningsperiode internt.
 * @param innvilgelsesperioder Vil kun være med på Søknadsvedtak med (delvis) innvilgelse og omgjøringsvedtak som gir (delvis) innvilgelse.
 * Per 2025-10-22 vil vi ikke ha mer enn 1 element i listen, men vi bygge inn denne funksjonaliteten.
 * @param omgjortAvRammevedtakId Dette vedtaket har blitt erstattet av et annet vedtak i sin helhet.
 * @param omgjorRammevedtakId Dette vedtaket erstatter et annet vedtak i sin helhet.
 */
data class VedtakDTO(
    val vedtakId: String,
    val rettighet: RettighetDTO,
    val periode: PeriodeDTO,
    val kilde: KildeDTO,
    val barnetillegg: BarnetilleggDTO?,
    val sats: Int?,
    val satsBarnetillegg: Int?,
    val vedtaksperiode: PeriodeDTO,
    val innvilgelsesperioder: List<PeriodeDTO>,
    val omgjortAvRammevedtakId: String?,
    val omgjorRammevedtakId: String?,
    val vedtakstidspunkt: OffsetDateTime?,
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

fun DatadelingsvedtakUtenAvslag.toVedtakDTO(): VedtakDTO {
    return VedtakDTO(
        vedtakId = vedtakId,
        rettighet = VedtakDTO.RettighetDTO.valueOf(rettighet.name),
        periode = periode.toDTO(),
        kilde = VedtakDTO.KildeDTO.valueOf(kilde.name),
        barnetillegg = barnetillegg?.toDTO(),
        sats = sats,
        satsBarnetillegg = satsBarnetillegg,
        vedtaksperiode = vedtaksperiode.toDTO(),
        innvilgelsesperioder = innvilgelsesperioder.map { it.toDTO() },
        omgjortAvRammevedtakId = omgjortAvRammevedtakId,
        omgjorRammevedtakId = omgjorRammevedtakId,
        vedtakstidspunkt = vedtakstidspunkt,
    )
}

fun List<DatadelingsvedtakUtenAvslag>.toVedtakDTO(): List<VedtakDTO> =
    map { it.toVedtakDTO() }

private fun DatadelingsvedtakUtenAvslag.Barnetillegg.toDTO(): VedtakDTO.BarnetilleggDTO {
    return VedtakDTO.BarnetilleggDTO(
        perioder = perioder.map {
            VedtakDTO.BarnetilleggDTO.BarnetilleggPeriodeDTO(
                antallBarn = it.antallBarn,
                periode = it.periode.toDTO(),
            )
        },
    )
}

private fun Periode.toDTO(): VedtakDTO.PeriodeDTO =
    VedtakDTO.PeriodeDTO(
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
    )
