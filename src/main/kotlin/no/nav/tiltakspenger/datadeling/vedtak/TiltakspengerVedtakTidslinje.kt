package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.AVSLAG
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.OPPHØR
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.STANS
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.TILTAKSPENGER
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje

/**
 * En periodisert liste over de gjeldende innvilgede vedtak i tp-sak.
 * Avslag er ekskludert fra tidslinjen. Og Stans/Opphør ekskluderes etter vi lager tidslinjen.
 * Vi fjerner også den delen av omgjøringsvedtak som ikke gir rett til tiltakspenger.
 */
fun List<TiltakspengerVedtak>.hentInnvilgetTidslinje(): Periodisering<TiltakspengerVedtak> {
    return hentTidslinje()
        .mapNotNull { (vedtak, gjeldendePeriode) ->
            when (vedtak.rettighet) {
                AVSLAG -> throw IllegalStateException("Avslag skal være filtrert vekk før innvilget tidslinje lages.")

                OPPHØR, STANS -> null

                TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG -> {
                    // Omgjøringsvedtak kan ha en innvilgelsesperiode som er mindre enn virkningsperioden (implisitt ikke lenger rett).
                    gjeldendePeriode.overlappendePeriode(vedtak.innvilgelsesperiode!!)?.let { overlappendePeriode ->
                        PeriodeMedVerdi(vedtak, overlappendePeriode)
                    }
                }
            }
        }.tilPeriodisering()
}

/**
 * En periodisert liste over gjeldende vedtak i tp-sak.
 * Avslag skal være ekskludert.
 * Fjerner vedtak som er omgjort i sin helhet.
 */
fun List<TiltakspengerVedtak>.hentTidslinje(): Periodisering<TiltakspengerVedtak> {
    return filter {
        when (it.rettighet) {
            TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG, STANS, OPPHØR -> true

            // Rene søknadsbehandlingsavslag påvirker ikke retten din til tiltakspenger.
            AVSLAG -> false
        }
    }
        // Fjerner alle vedtak som er omgjort i sin helhet av et annet vedtak.
        .filter { it.omgjortAvRammevedtakId == null }
        .toTidslinje()
}
