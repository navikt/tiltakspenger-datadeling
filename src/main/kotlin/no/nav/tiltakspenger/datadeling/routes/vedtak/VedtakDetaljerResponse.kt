package no.nav.tiltakspenger.datadeling.routes.vedtak

import io.github.oshai.kotlinlogging.KLogger
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
    val sats: Int?,
    val satsBarnetillegg: Int?,
) {
    enum class RettighetResponseJson {
        TILTAKSPENGER,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }
}

internal fun List<TiltakspengerVedtak>.toVedtakDetaljerResponse(log: KLogger): List<VedtakDetaljerResponse> {
    return this.map { it.toVedtakDetaljerResponse(log) }
}

internal fun TiltakspengerVedtak.toVedtakDetaljerResponse(log: KLogger): VedtakDetaljerResponse {
    val satser = this.getSatser(log)
    return VedtakDetaljerResponse(
        fom = this.periode.fraOgMed,
        tom = this.periode.tilOgMed,
        rettighet = when (this.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> RettighetResponseJson.TILTAKSPENGER
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> RettighetResponseJson.TILTAKSPENGER_OG_BARNETILLEGG
            TiltakspengerVedtak.Rettighet.STANS -> RettighetResponseJson.INGENTING
        },
        vedtakId = this.vedtakId,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        kilde = this.kilde.navn,
        sats = satser?.sats,
        satsBarnetillegg = satser?.let {
            if (rettighet == TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG) {
                it.satsBarnetillegg
            } else {
                0
            }
        },
    )
}
