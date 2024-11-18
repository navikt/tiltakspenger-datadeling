package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.libs.json.serialize
import java.time.LocalDate

/**
 * Kontrakt for vedtakstidslinje. Brukes blant annet av Modia personbruker.
 */
private data class VedtakTidslinjeResponseJson(
    val fom: LocalDate,
    val tom: LocalDate,
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
    return VedtakTidslinjeResponseJson(
        fom = this.periode.fraOgMed,
        tom = this.periode.tilOgMed,
        rettighet = when (this.rettighet) {
            Vedtak.Rettighet.TILTAKSPENGER -> VedtakTidslinjeResponseJson.RettighetResponseJson.TILTAKSPENGER
            Vedtak.Rettighet.BARNETILLEGG -> VedtakTidslinjeResponseJson.RettighetResponseJson.BARNETILLEGG
            Vedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> VedtakTidslinjeResponseJson.RettighetResponseJson.TILTAKSPENGER_OG_BARNETILLEGG
            Vedtak.Rettighet.INGENTING -> VedtakTidslinjeResponseJson.RettighetResponseJson.INGENTING
        },
        vedtakId = this.vedtakId,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        kilde = this.kilde,
    ).let { serialize(it) }
}
