package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.routes.VedtakResponseJson.RettighetResponseJson
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
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
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }
}

internal fun Periodisering<TiltakspengerVedtak>.toJson(): String {
    return this.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toJson() }
}

internal fun PeriodeMedVerdi<TiltakspengerVedtak>.toJson(): String {
    return VedtakResponseJson(
        fom = this.periode.fraOgMed,
        tom = this.periode.tilOgMed,
        rettighet = when (this.verdi.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> RettighetResponseJson.TILTAKSPENGER
            TiltakspengerVedtak.Rettighet.INGENTING -> RettighetResponseJson.INGENTING
        },
        vedtakId = this.verdi.vedtakId,
        sakId = this.verdi.sakId,
        saksnummer = this.verdi.saksnummer,
        kilde = this.verdi.kilde,
    ).let { serialize(it) }
}
