package no.nav.tiltakspenger.datadeling.felles

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime

object VedtakMother {
    fun tiltakspengerVedtak(
        fom: LocalDate = LocalDate.of(2024, 1, 1),
        tom: LocalDate = LocalDate.of(2024, 1, 31),
        antallDagerPerMeldeperiode: Int = 10,
        meldeperiodensLengde: Int = 14,
        dagsatsTiltakspenger: Int = 1000,
        dagsatsBarnetillegg: Int = 0,
        antallBarn: Int = 0,
        tiltaksgjennomføringId: String = "tiltaksgjennomføringId",
        rettighet: TiltakspengerVedtak.Rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        vedtakId: String = "vedtakId",
        sakId: String = "sakId",
        saksnummer: String = "saksnummer",
        kilde: String = "kilde",
        fnr: Fnr = Fnr.fromString("12345678901"),
        mottattTidspunkt: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000"),
        opprettetTidspunkt: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000"),
    ) = TiltakspengerVedtak(
        periode = Periode(fom, tom),
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        meldeperiodensLengde = meldeperiodensLengde,
        dagsatsTiltakspenger = dagsatsTiltakspenger,
        dagsatsBarnetillegg = dagsatsBarnetillegg,
        antallBarn = antallBarn,
        tiltaksgjennomføringId = tiltaksgjennomføringId,
        rettighet = rettighet,
        vedtakId = vedtakId,
        sakId = sakId,
        saksnummer = saksnummer,
        kilde = kilde,
        fnr = fnr,
        mottattTidspunkt = mottattTidspunkt,
        opprettetTidspunkt = opprettetTidspunkt,
    )
}
