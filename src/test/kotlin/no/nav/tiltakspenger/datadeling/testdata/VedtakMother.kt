package no.nav.tiltakspenger.datadeling.testdata

import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import java.time.LocalDateTime
import java.util.UUID

object VedtakMother {
    fun tiltakspengerVedtak(
        virkningsperiode: Periode = (1 til 31.januar(2024)),
        rettighet: TiltakspengerVedtak.Rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        innvilgelsesperiode: Periode? = when (rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            -> virkningsperiode

            TiltakspengerVedtak.Rettighet.STANS,
            TiltakspengerVedtak.Rettighet.AVSLAG,
            -> null
        },
        omgjortAvRammevedtakId: String? = null,
        omgjørRammevedtakId: String? = null,
        vedtakId: String = UUID.randomUUID().toString(),
        sakId: String = "sakId",
        saksnummer: String = "saksnummer",
        fnr: Fnr = Fnr.fromString("12345678901"),
        mottattTidspunkt: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000"),
        opprettetTidspunkt: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000"),
        barnetillegg: Barnetillegg? = null,
        valgteHjemlerHarIkkeRettighet: List<TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet>? = null,
    ) = TiltakspengerVedtak(
        virkningsperiode = virkningsperiode,
        innvilgelsesperiode = innvilgelsesperiode,
        omgjortAvRammevedtakId = omgjortAvRammevedtakId,
        omgjørRammevedtakId = omgjørRammevedtakId,
        rettighet = rettighet,
        vedtakId = vedtakId,
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        mottattTidspunkt = mottattTidspunkt,
        opprettet = opprettetTidspunkt,
        barnetillegg = barnetillegg,
        valgteHjemlerHarIkkeRettighet = valgteHjemlerHarIkkeRettighet,
    )
}
