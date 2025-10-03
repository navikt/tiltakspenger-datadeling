package no.nav.tiltakspenger.datadeling.testdata

import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object BehandlingMother {
    fun tiltakspengerBehandling(
        fom: LocalDate = LocalDate.of(2024, 1, 1),
        tom: LocalDate = LocalDate.of(2024, 1, 31),
        behandlingId: String = UUID.randomUUID().toString(),
        behandlingStatus: TiltakspengerBehandling.Behandlingsstatus = TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING,
        saksbehandler: String? = "testSaksbehandler",
        beslutter: String? = "testBeslutter",
        iverksattTidspunkt: LocalDateTime? = LocalDateTime.parse("2021-01-01T00:00:00.000"),
        søknadJournalpostId: String = "testSøknadJournalpostId",
        sakId: String = "sakId",
        saksnummer: String = "saksnummer",
        fnr: Fnr = Fnr.Companion.fromString("12345678901"),
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
        søknadJournalpostId = søknadJournalpostId,
        opprettetTidspunktSaksbehandlingApi = opprettetTidspunktSaksbehandlingApi,
        mottattTidspunktDatadeling = mottattTidspunktDatadeling,
    )
}
