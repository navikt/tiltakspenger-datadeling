package no.nav.tiltakspenger.datadeling.client.arena.domene

import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.libs.periode.Periode

data class PeriodisertKilde(
    val periode: Periode,
    val kilde: Kilde,
)
