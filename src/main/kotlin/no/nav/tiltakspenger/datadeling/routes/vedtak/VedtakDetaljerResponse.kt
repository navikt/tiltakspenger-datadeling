package no.nav.tiltakspenger.datadeling.routes.vedtak

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.routes.vedtak.VedtakDetaljerResponse.RettighetResponseJson
import java.time.LocalDate

/**
 * Kontrakt for vedtaksdetaljer. Brukes av Arena.
 */
data class VedtakDetaljerResponse(
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

internal fun List<TiltakspengerVedtak>.toVedtakDetaljerResponse(): List<VedtakDetaljerResponse> {
    return this.map { it.toVedtakDetaljerResponse() }
}

internal fun TiltakspengerVedtak.toVedtakDetaljerResponse(): VedtakDetaljerResponse {
    return VedtakDetaljerResponse(
        fom = this.periode.fraOgMed,
        tom = this.periode.tilOgMed,
        rettighet = when (this.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> RettighetResponseJson.TILTAKSPENGER
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> RettighetResponseJson.TILTAKSPENGER_OG_BARNETILLEGG
            TiltakspengerVedtak.Rettighet.INGENTING -> RettighetResponseJson.INGENTING
        },
        vedtakId = this.vedtakId,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        kilde = this.kilde.navn,
    )
}
