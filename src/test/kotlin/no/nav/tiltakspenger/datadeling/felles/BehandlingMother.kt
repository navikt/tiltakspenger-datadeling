package no.nav.tiltakspenger.datadeling.felles

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime

object BehandlingMother {
    fun tiltakspengerBehandling(
        fom: LocalDate = LocalDate.of(2024, 1, 1),
        tom: LocalDate = LocalDate.of(2024, 1, 31),
        behandlingId: String = "testBehandlingId",
        behandlingStatus: TiltakspengerBehandling.Behandlingsstatus = TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING,
        saksbehandler: String? = "testSaksbehandler",
        beslutter: String? = "testBeslutter",
        iverksattTidspunkt: LocalDateTime? = LocalDateTime.parse("2021-01-01T00:00:00.000"),
        tiltaksdeltagelse: TiltakspengerBehandling.Tiltaksdeltagelse = TiltakspengerBehandling.Tiltaksdeltagelse(
            tiltaksnavn = "testTiltaksnavn",
            eksternTiltaksdeltakerId = "testEksternTiltaksdeltakerId",
            eksternGjennomføringId = "testEksternGjennomføringId",
        ),
        søknadJournalpostId: String = "testSøknadJournalpostId",
        sakId: String = "sakId",
        saksnummer: String = "saksnummer",
        fnr: Fnr = Fnr.fromString("12345678901"),
        opprettetTidspunktSaksbehandlingApi: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000"),
        mottattTidspunktDatadeling: LocalDateTime = LocalDateTime.parse("2021-01-01T00:00:00.000"),
    ): TiltakspengerBehandling = TiltakspengerBehandling(
        periode = Periode(fom, tom),
        fnr = fnr,
        sakId = sakId,
        saksnummer = saksnummer,
        behandlingId = behandlingId,
        behandlingStatus = behandlingStatus,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        iverksattTidspunkt = iverksattTidspunkt,
        tiltaksdeltagelse = tiltaksdeltagelse,
        søknadJournalpostId = søknadJournalpostId,
        opprettetTidspunktSaksbehandlingApi = opprettetTidspunktSaksbehandlingApi,
        mottattTidspunktDatadeling = mottattTidspunktDatadeling,
    )
}