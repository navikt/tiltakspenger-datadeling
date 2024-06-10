package no.nav.tiltakspenger.datadeling.domene

import java.time.LocalDate

data class Vedtak(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDager: Double,
    val dagsatsTiltakspenger: Int,
    val dagsatsBarnetillegg: Int,
    val antallBarn: Int,
)
