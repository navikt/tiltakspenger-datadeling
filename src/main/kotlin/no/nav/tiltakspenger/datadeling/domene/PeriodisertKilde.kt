package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode

data class PeriodisertKilde(
    val periode: Periode,
    val kilde: String,
)
