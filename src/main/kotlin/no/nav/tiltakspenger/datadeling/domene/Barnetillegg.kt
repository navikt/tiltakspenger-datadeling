package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode

data class Barnetillegg(
    val perioder: List<BarnetilleggPeriode>,
)

data class BarnetilleggPeriode(
    val antallBarn: Int,
    val periode: Periode,
)
