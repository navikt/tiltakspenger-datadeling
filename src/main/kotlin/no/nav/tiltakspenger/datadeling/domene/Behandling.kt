package no.nav.tiltakspenger.datadeling.domene

import java.time.LocalDate

data class Behandling(
    val behandlingId: String,
    val fom: LocalDate,
    val tom: LocalDate,
)
