package no.nav.tiltakspenger.datadeling.vedtak

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.datadeling.Kilde
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import java.time.OffsetDateTime

/**
 * Felles domenemodell for vedtak fra Arena og tp-sak som kan deles videre uten rene avslag.
 *
 * Modellen brukes før infra-laget mapper videre til endepunktspesifikke DTO-er.
 * Den inneholder derfor noen domenevalg som ikke bør bo i route-laget:
 * - Rene avslag representeres ikke her.
 *   Endepunktene som bruker denne modellen skal dele vedtak som påvirker eller beskriver rettighetsperioder, mens avslag håndteres av modeller/endepunkt som eksplisitt støtter avslag.
 * - Stans og opphør normaliseres til [Rettighet.INGENTING], siden de uttrykker at bruker ikke har rett i perioden.
 *
 * Avslag fra tp-sak skal filtreres ut før mapping til denne modellen.
 * Hvis et avslag likevel forsøkes mappet hit, feiler vi tydelig for å unngå at route-laget må kjenne til dette domeneskillet.
 */
data class DatadelingsvedtakUtenAvslag(
    val vedtakId: String,
    val rettighet: Rettighet,
    val periode: Periode,
    val kilde: Kilde,
    val barnetillegg: Barnetillegg?,
    val sats: Int?,
    val satsBarnetillegg: Int?,
    val vedtaksperiode: Periode,
    val innvilgelsesperioder: List<Periode>,
    val omgjortAvRammevedtakId: String?,
    val omgjorRammevedtakId: String?,
    val vedtakstidspunkt: OffsetDateTime?,
) {
    enum class Rettighet {
        TILTAKSPENGER,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }

    enum class Kilde {
        TPSAK,
        ARENA,
    }

    data class Barnetillegg(
        val perioder: List<BarnetilleggPeriode>,
    ) {
        data class BarnetilleggPeriode(
            val antallBarn: Int,
            val periode: Periode,
        )
    }
}

fun TiltakspengerVedtak.toDatadelingsvedtakUtenAvslag(log: KLogger): DatadelingsvedtakUtenAvslag {
    val satser = this.getSatser(log)
    return DatadelingsvedtakUtenAvslag(
        vedtakId = vedtakId,
        rettighet = when (rettighet) {
            TiltakspengerVedtak.Rettighet.STANS,
            TiltakspengerVedtak.Rettighet.OPPHØR,
            -> DatadelingsvedtakUtenAvslag.Rettighet.INGENTING

            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> DatadelingsvedtakUtenAvslag.Rettighet.TILTAKSPENGER

            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> DatadelingsvedtakUtenAvslag.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG

            TiltakspengerVedtak.Rettighet.AVSLAG -> throw IllegalStateException("Dette apiet skal ikke returnere avslag")
        },
        periode = when (rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            -> innvilgelsesperiode!!

            TiltakspengerVedtak.Rettighet.STANS,
            TiltakspengerVedtak.Rettighet.OPPHØR,
            -> virkningsperiode

            TiltakspengerVedtak.Rettighet.AVSLAG -> throw IllegalStateException("Dette apiet skal ikke returnere avslag")
        },
        kilde = DatadelingsvedtakUtenAvslag.Kilde.TPSAK,
        barnetillegg = barnetillegg?.toDatadelingsvedtakUtenAvslagBarnetillegg(),
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
        vedtakstidspunkt = this.opprettet.atZone(zoneIdOslo).toOffsetDateTime(),
    )
}

fun ArenaVedtak.toDatadelingsvedtakUtenAvslag(): DatadelingsvedtakUtenAvslag {
    val rettighet = DatadelingsvedtakUtenAvslag.Rettighet.valueOf(rettighet.name)
    return DatadelingsvedtakUtenAvslag(
        vedtakId = vedtakId,
        rettighet = rettighet,
        periode = periode,
        kilde = when (kilde) {
            Kilde.TPSAK -> DatadelingsvedtakUtenAvslag.Kilde.TPSAK
            Kilde.ARENA -> DatadelingsvedtakUtenAvslag.Kilde.ARENA
        },
        barnetillegg = if (this.rettighet == Rettighet.TILTAKSPENGER_OG_BARNETILLEGG && antallBarn > 0) {
            DatadelingsvedtakUtenAvslag.Barnetillegg(
                perioder = listOf(
                    DatadelingsvedtakUtenAvslag.Barnetillegg.BarnetilleggPeriode(
                        antallBarn = antallBarn,
                        periode = periode,
                    ),
                ),
            )
        } else {
            null
        },
        sats = dagsatsTiltakspenger,
        satsBarnetillegg = dagsatsBarnetillegg,
        vedtaksperiode = periode,
        innvilgelsesperioder = when (rettighet) {
            DatadelingsvedtakUtenAvslag.Rettighet.TILTAKSPENGER,
            DatadelingsvedtakUtenAvslag.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            -> listOf(periode)

            DatadelingsvedtakUtenAvslag.Rettighet.INGENTING -> emptyList()
        },
        omgjortAvRammevedtakId = null,
        omgjorRammevedtakId = null,
        vedtakstidspunkt = beslutningsdato?.atTime(9, 0)?.atZone(zoneIdOslo)?.toOffsetDateTime(),
    )
}

private fun Barnetillegg.toDatadelingsvedtakUtenAvslagBarnetillegg(): DatadelingsvedtakUtenAvslag.Barnetillegg {
    return DatadelingsvedtakUtenAvslag.Barnetillegg(
        perioder = this.perioder.map {
            DatadelingsvedtakUtenAvslag.Barnetillegg.BarnetilleggPeriode(
                antallBarn = it.antallBarn,
                periode = it.periode,
            )
        },
    )
}
