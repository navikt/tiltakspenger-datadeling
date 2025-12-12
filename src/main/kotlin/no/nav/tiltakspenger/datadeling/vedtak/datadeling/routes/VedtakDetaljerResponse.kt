package no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakDetaljerResponse.RettighetResponseJson
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
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

internal fun List<TiltakspengeVedtakMedSak>.toVedtakDetaljerResponse(log: KLogger): List<VedtakDetaljerResponse> {
    return this.map { it.toVedtakDetaljerResponse(log) }
}

internal fun TiltakspengeVedtakMedSak.toVedtakDetaljerResponse(log: KLogger): VedtakDetaljerResponse {
    val satser = this.vedtak.getSatser(log)
    return VedtakDetaljerResponse(
        // Stans og avslag er filtrert vekk. Arena ønsker bare de innvilgede periodene eller en tom liste dersom ingen.
        fom = this.vedtak.innvilgelsesperiode?.fraOgMed ?: this.vedtak.virkningsperiode.fraOgMed,
        tom = this.vedtak.innvilgelsesperiode?.tilOgMed ?: this.vedtak.virkningsperiode.tilOgMed,
        rettighet = when (this.vedtak.rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER -> RettighetResponseJson.TILTAKSPENGER
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> RettighetResponseJson.TILTAKSPENGER_OG_BARNETILLEGG
            TiltakspengerVedtak.Rettighet.STANS -> RettighetResponseJson.INGENTING
            TiltakspengerVedtak.Rettighet.AVSLAG -> throw IllegalStateException("Dette apiet skal ikke returnere avslag")
        },
        vedtakId = this.vedtak.vedtakId,
        sakId = this.vedtak.sakId,
        saksnummer = this.sak.saksnummer,
        kilde = Kilde.TPSAK.navn,
        sats = satser?.sats,
        satsBarnetillegg = satser?.let {
            if (vedtak.rettighet == TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG) {
                it.satsBarnetillegg
            } else {
                0
            }
        },
    )
}
