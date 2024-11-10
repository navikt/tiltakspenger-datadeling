package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.libs.json.serialize

private data class PerioderResponseJson(
    val fom: String,
    val tom: String,
    val kilde: String,
)

internal fun List<PeriodisertKilde>.toJson(): String {
    return this.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toJson() }
}

internal fun PeriodisertKilde.toJson(): String {
    return serialize(
        PerioderResponseJson(
            fom = this.periode.fraOgMed.toString(),
            tom = this.periode.tilOgMed.toString(),
            kilde = this.kilde,
        ),
    )
}
