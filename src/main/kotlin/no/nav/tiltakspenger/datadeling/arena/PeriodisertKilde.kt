package no.nav.tiltakspenger.datadeling.arena

import no.nav.tiltakspenger.datadeling.Kilde
import no.nav.tiltakspenger.libs.periode.Periode

data class PeriodisertKilde(
    val periode: Periode,
    val kilde: Kilde,
)
