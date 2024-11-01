package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode

data class PeriodisertKilde(
    val periode: Periode,
    // TODO post-mvp: Litt rart og ha kilde i en generell periodetype. Kunne disse vært delt opp i 2 typer?
    val kilde: String,
)
