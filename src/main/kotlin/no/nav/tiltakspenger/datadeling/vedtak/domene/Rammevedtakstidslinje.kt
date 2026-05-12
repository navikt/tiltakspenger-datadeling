package no.nav.tiltakspenger.datadeling.vedtak.domene

import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.AVSLAG
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.OPPHØR
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.STANS
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.TILTAKSPENGER
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje

/**
 * En periodisert liste over gjeldende rammevedtak.
 * Avslag ekskluderes - rene søknadsbehandlingsavslag påvirker ikke retten til tiltakspenger.
 * Vedtak som er omgjort i sin helhet av et annet vedtak ekskluderes også.
 */
fun List<TiltakspengerVedtak>.tilRammevedtakstidslinje(): Periodisering<TiltakspengerVedtak> =
    this
        .filter {
            when (it.rettighet) {
                TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG, STANS, OPPHØR -> true
                AVSLAG -> false
            }
        }
        .toTidslinje()

/**
 * En periodisert liste over de gjeldende innvilgede rammevedtakene.
 * Avslag, stans og opphør ekskluderes. Vedtak som er omgjort i sin helhet ekskluderes.
 * Omgjøringsvedtak krympes til den delen som overlapper med innvilgelsesperioden,
 * siden et omgjøringsvedtak kan ha en innvilgelsesperiode som er mindre enn virkningsperioden
 * (resten er da implisitt ikke lenger gir rett).
 */
fun List<TiltakspengerVedtak>.tilInnvilgetRammevedtakstidslinje(): Periodisering<TiltakspengerVedtak> {
    return this.tilRammevedtakstidslinje()
        .mapNotNull { (vedtak, gjeldendePeriode) ->
            when (vedtak.rettighet) {
                AVSLAG -> throw IllegalStateException(
                    "Avslag skal være filtrert vekk før innvilget tidslinje lages. vedtakId=${vedtak.vedtakId}",
                )

                OPPHØR, STANS -> null

                TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG ->
                    gjeldendePeriode.overlappendePeriode(vedtak.innvilgelsesperiode!!)
                        ?.let { PeriodeMedVerdi(vedtak, it) }
            }
        }
        .tilPeriodisering()
}
