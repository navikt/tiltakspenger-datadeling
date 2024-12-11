package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering

fun List<TiltakspengerVedtak>.toTidslinje(): Periodisering<TiltakspengerVedtak> {
    if (this.isEmpty()) return Periodisering(emptyList())

    val sortedByDescending = this.sortedByDescending { it.opprettetTidspunkt }
    return sortedByDescending
        .drop(1)
        .fold(
            Periodisering(sortedByDescending.first(), sortedByDescending.first().periode),
        ) { akkumulerteVedtak, vedtak ->
            akkumulerteVedtak.utvid(vedtak, vedtak.periode)
        }
}

fun <T> Periodisering<T>.utvid2(
    verdi: T,
    nyeTotalePeriode: Periode,
): Periodisering<T> {
    val nyePerioder = nyeTotalePeriode.trekkFra(listOf(totalePeriode))
    return this.copy(
        perioderMedVerdi =
        (this.perioderMedVerdi + nyePerioder.map { PeriodeMedVerdi(verdi, it) })
            .sortedBy { it.periode.fraOgMed },
    )
}
