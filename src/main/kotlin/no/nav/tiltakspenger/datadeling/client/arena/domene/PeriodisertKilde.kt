package no.nav.tiltakspenger.datadeling.client.arena.domene

import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.libs.periodisering.Periode

data class PeriodisertKilde(
    val periode: Periode,
    val kilde: Kilde,
)
