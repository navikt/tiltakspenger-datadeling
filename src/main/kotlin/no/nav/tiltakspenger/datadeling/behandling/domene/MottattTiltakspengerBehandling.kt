package no.nav.tiltakspenger.datadeling.behandling.domene

import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDateTime

data class MottattTiltakspengerBehandling(
    val behandlingId: String,
    val sakId: String,
    val periode: Periode?,
    val behandlingStatus: TiltakspengerBehandling.Behandlingsstatus,
    val saksbehandler: String?,
    val beslutter: String?,
    val iverksattTidspunkt: LocalDateTime?,
    val opprettetTidspunktSaksbehandlingApi: LocalDateTime,
    val mottattTidspunktDatadeling: LocalDateTime,
    val behandlingstype: TiltakspengerBehandling.Behandlingstype,
    val sistEndret: LocalDateTime,
) {
    init {
        require(sakId.isNotBlank()) { "sakId kan ikke være blank" }
    }

    fun medSak(sak: Sak): TiltakspengerBehandling {
        require(sak.id == sakId) { "Kan ikke berike behandling $behandlingId med feil sak ${sak.id}" }
        return TiltakspengerBehandling(
            behandlingId = behandlingId,
            sakId = sakId,
            periode = periode,
            behandlingStatus = behandlingStatus,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            iverksattTidspunkt = iverksattTidspunkt,
            opprettetTidspunktSaksbehandlingApi = opprettetTidspunktSaksbehandlingApi,
            mottattTidspunktDatadeling = mottattTidspunktDatadeling,
            behandlingstype = behandlingstype,
            sistEndret = sistEndret,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
        )
    }
}
