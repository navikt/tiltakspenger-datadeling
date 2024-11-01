package no.nav.tiltakspenger.datadeling.felles

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import java.time.LocalDate
import java.time.LocalDateTime

object VedtakMother {
    fun tiltakspengerVedtak(
        fom: LocalDate = LocalDate.of(2024, 1, 1),
        tom: LocalDate = LocalDate.of(2021, 1, 31),
        antallDager: Int = 31,
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
        mottattTidspunkt: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000Z"),
        opprettetTidspunkt: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000Z")
    ) = TiltakspengerVedtak(
        fom = fom,
        tom = tom,
        antallDagerPerMeldeperiode = antallDager,
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
        opprettetTidspunkt = opprettetTidspunkt
    )
}