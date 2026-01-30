package no.nav.tiltakspenger.datadeling.vedtak.domene

import no.nav.tiltakspenger.libs.periode.Periode

data class Barnetillegg(
    val perioder: List<BarnetilleggPeriode>,
) {
    /** Krymper nåværende periode til denne, eller fjerner den dersom den ikke overlapper. */
    fun oppdaterPeriode(nyPeriode: Periode): Barnetillegg {
        return this.copy(perioder = perioder.mapNotNull { it.oppdaterPeriode(nyPeriode) })
    }
}

data class BarnetilleggPeriode(
    val antallBarn: Int,
    val periode: Periode,
) {
    fun oppdaterPeriode(nyPeriode: Periode): BarnetilleggPeriode? {
        return periode.overlappendePeriode(nyPeriode)?.let { this.copy(periode = it) }
    }
}
