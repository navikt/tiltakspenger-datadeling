package no.nav.tiltakspenger.datadeling.domene

import java.time.LocalDate

data class Barnetillegg(
    val perioder: List<BarnetilleggPeriode>,
)

data class BarnetilleggPeriode(
    val antallBarn: Int,
    val periode: Periode,
) {
    data class Periode(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )
}
