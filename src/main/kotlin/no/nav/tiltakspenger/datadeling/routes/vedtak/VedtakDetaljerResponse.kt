package no.nav.tiltakspenger.datadeling.routes.vedtak

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.routes.vedtak.VedtakDetaljerResponse.RettighetResponseJson
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
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

internal fun Periodisering<TiltakspengerVedtak>.toVedtakDetaljerResponse(): List<VedtakDetaljerResponse> {
    return this.perioderMedVerdi.map { it.toVedtakDetaljerResponse() }
}

internal fun PeriodeMedVerdi<TiltakspengerVedtak>.toVedtakDetaljerResponse(): VedtakDetaljerResponse {
    return VedtakDetaljerResponse(
        fom = this.periode.fraOgMed,
        tom = this.periode.tilOgMed,
        rettighet = when (this.verdi.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> RettighetResponseJson.TILTAKSPENGER
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> RettighetResponseJson.TILTAKSPENGER_OG_BARNETILLEGG
            TiltakspengerVedtak.Rettighet.INGENTING -> RettighetResponseJson.INGENTING
        },
        vedtakId = this.verdi.vedtakId,
        sakId = this.verdi.sakId,
        saksnummer = this.verdi.saksnummer,
        kilde = this.verdi.kilde.navn,
    )
}
