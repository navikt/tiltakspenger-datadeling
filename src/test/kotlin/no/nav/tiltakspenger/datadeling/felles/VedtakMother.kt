package no.nav.tiltakspenger.datadeling.felles

import no.nav.tiltakspenger.datadeling.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object VedtakMother {
    fun tiltakspengerVedtak(
        fom: LocalDate = LocalDate.of(2024, 1, 1),
        tom: LocalDate = LocalDate.of(2024, 1, 31),
        antallDagerPerMeldeperiode: Int = 10,
        rettighet: TiltakspengerVedtak.Rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        vedtakId: String = UUID.randomUUID().toString(),
        sakId: String = "sakId",
        saksnummer: String = "saksnummer",
        fnr: Fnr = Fnr.fromString("12345678901"),
        mottattTidspunkt: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000"),
        opprettetTidspunkt: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000"),
        barnetillegg: Barnetillegg? = null,
        valgteHjemlerHarIkkeRettighet: List<TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet>? = null,
    ) = TiltakspengerVedtak(
        periode = Periode(fom, tom),
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
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
