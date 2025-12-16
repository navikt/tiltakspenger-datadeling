package no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaVedtak
import no.nav.tiltakspenger.datadeling.client.arena.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Kontrakt for vedtaksperioder. Brukes av modia-personoversikt.
 * @param periode deprecated - bruk vedtaksperiode + innvilgelsesperioder
 * @param vedtaksperiode kan være større enn innvilgelsesperioden ved omgjøring. Kalles virkningsperiode internt.
 * @param innvilgelsesperioder Vil kun være med på Søknadsvedtak med (delvis) innvilgelse og omgjøringsvedtak som gir (delvis) innvilgelse. Per 2025-10-22 vil vi ikke ha mer enn 1 element i listen, men vi bygge inn denne funksjonaliteten.
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

fun TiltakspengerVedtak.toVedtakDTO(log: KLogger): VedtakDTO {
    val satser = this.getSatser(log)
    return VedtakDTO(
        vedtakId = vedtakId,
        rettighet = when (rettighet) {
            TiltakspengerVedtak.Rettighet.STANS -> VedtakDTO.RettighetDTO.INGENTING
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> VedtakDTO.RettighetDTO.TILTAKSPENGER
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> VedtakDTO.RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG
            TiltakspengerVedtak.Rettighet.AVSLAG -> throw IllegalStateException("Dette apiet skal ikke returnere avslag")
        },
        periode = when (rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER, TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> innvilgelsesperiode!!
            TiltakspengerVedtak.Rettighet.STANS -> virkningsperiode
            TiltakspengerVedtak.Rettighet.AVSLAG -> throw IllegalStateException("Dette apiet skal ikke returnere avslag")
        }.let { VedtakDTO.PeriodeDTO(it.fraOgMed, it.tilOgMed) },
        kilde = VedtakDTO.KildeDTO.TPSAK,
        barnetillegg = barnetillegg?.toDTO(),
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
        vedtakstidspunkt = this.opprettet.atZone(zoneIdOslo).toOffsetDateTime(),
    )
}

fun ArenaVedtak.toVedtakDTO(): VedtakDTO {
    val rettighet: VedtakDTO.RettighetDTO = VedtakDTO.RettighetDTO.valueOf(rettighet.name)
    return VedtakDTO(
        vedtakId = vedtakId,
        rettighet = rettighet,
        periode = VedtakDTO.PeriodeDTO(
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
        ),
        kilde = when (kilde) {
            Kilde.TPSAK -> VedtakDTO.KildeDTO.TPSAK
            Kilde.ARENA -> VedtakDTO.KildeDTO.ARENA
        },
        barnetillegg = if (this.rettighet == Rettighet.TILTAKSPENGER_OG_BARNETILLEGG && antallBarn > 0) {
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
        sats = dagsatsTiltakspenger,
        satsBarnetillegg = dagsatsBarnetillegg,
        vedtaksperiode = VedtakDTO.PeriodeDTO(
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
        ),
        innvilgelsesperioder = when (rettighet) {
            VedtakDTO.RettighetDTO.TILTAKSPENGER,
            VedtakDTO.RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG,
            -> listOf(
                VedtakDTO.PeriodeDTO(
                    fraOgMed = periode.fraOgMed,
                    tilOgMed = periode.tilOgMed,
                ),
            )

            VedtakDTO.RettighetDTO.INGENTING -> emptyList()
        },
        omgjorRammevedtakId = null,
        omgjortAvRammevedtakId = null,
        vedtakstidspunkt = beslutningsdato?.atTime(9, 0)?.atZone(zoneIdOslo)?.toOffsetDateTime(),
    )
}
