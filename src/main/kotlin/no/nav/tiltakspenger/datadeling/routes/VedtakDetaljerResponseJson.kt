package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
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

internal fun List<TiltakspengerVedtak>.toJson(): String {
    return this.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toJson() }
}

internal fun TiltakspengerVedtak.toJson(): String {
    return VedtakResponseJson(
        fom = this.periode.fraOgMed,
        tom = this.periode.tilOgMed,
        rettighet = when (this.rettighet) {
            Vedtak.Rettighet.TILTAKSPENGER -> RettighetResponseJson.TILTAKSPENGER
            Vedtak.Rettighet.BARNETILLEGG -> RettighetResponseJson.BARNETILLEGG
            Vedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> RettighetResponseJson.TILTAKSPENGER_OG_BARNETILLEGG
            Vedtak.Rettighet.INGENTING -> RettighetResponseJson.INGENTING
        },
        vedtakId = this.vedtakId,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        kilde = this.kilde,
    ).let { serialize(it) }
}
