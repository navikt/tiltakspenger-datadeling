package no.nav.tiltakspenger.datadeling.domene

import java.time.LocalDate

data class Behandling(
    val fom: LocalDate,
    val tom: LocalDate,
)
