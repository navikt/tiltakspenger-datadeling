package no.nav.tiltakspenger.datadeling.infra.db

import no.nav.tiltakspenger.datadeling.vedtak.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.BarnetilleggPeriode

/**
 * Skal kun brukes i db-laget for serialisering/deserialisering av [Barnetillegg].
 * Vi serialiserer ikke domene-[no.nav.tiltakspenger.libs.periode.Periode] direkte; bruk [PeriodeDbJson].
 */
data class BarnetilleggDbJson(
    val perioder: List<BarnetilleggPeriodeDbJson>,
) {
    data class BarnetilleggPeriodeDbJson(
        val antallBarn: Int,
        val periode: PeriodeDbJson,
    )

    fun toDomain(): Barnetillegg = Barnetillegg(
        perioder = perioder.map {
            BarnetilleggPeriode(
                antallBarn = it.antallBarn,
                periode = it.periode.toDomain(),
            )
        },
    )
}

fun Barnetillegg.toDbJson(): BarnetilleggDbJson = BarnetilleggDbJson(
    perioder = perioder.map {
        BarnetilleggDbJson.BarnetilleggPeriodeDbJson(
            antallBarn = it.antallBarn,
            periode = it.periode.toDbJson(),
        )
    },
)
