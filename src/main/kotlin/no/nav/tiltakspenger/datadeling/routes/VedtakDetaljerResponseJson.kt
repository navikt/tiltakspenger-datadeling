package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.routes.VedtakResponseJson.RettighetResponseJson
import no.nav.tiltakspenger.libs.json.serialize
import java.time.LocalDate

/**
 * Kontrakt for vedtaksdetaljer. Brukes av Arena.
 */
private data class VedtakResponseJson(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDager: Double,
    val dagsatsTiltakspenger: Int,
    val dagsatsBarnetillegg: Int,
    val antallBarn: Int,
    val relaterteTiltak: String,
    val rettighet: RettighetResponseJson,
    val vedtakId: String,
    val sakId: String,
    val saksnummer: String?,
    val kilde: String,
) {
    enum class RettighetResponseJson {
        TILTAKSPENGER,
        BARNETILLEGG,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }
}

internal fun List<Vedtak>.toJson(): String {
    return this.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toJson() }
}

internal fun Vedtak.toJson(): String {
    return VedtakResponseJson(
        fom = this.fom,
        tom = this.tom,
        antallDager = this.antallDager,
        dagsatsTiltakspenger = this.dagsatsTiltakspenger,
        dagsatsBarnetillegg = this.dagsatsBarnetillegg,
        antallBarn = this.antallBarn,
        relaterteTiltak = this.tiltaksgjennomføringId,
        rettighet = when (this.rettighet) {
            Rettighet.TILTAKSPENGER -> RettighetResponseJson.TILTAKSPENGER
            Rettighet.BARNETILLEGG -> RettighetResponseJson.BARNETILLEGG
            Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> RettighetResponseJson.TILTAKSPENGER_OG_BARNETILLEGG
            Rettighet.INGENTING -> RettighetResponseJson.INGENTING
        },
        vedtakId = this.vedtakId,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        kilde = this.kilde,
    ).let { serialize(it) }
}
