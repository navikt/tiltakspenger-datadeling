package no.nav.tiltakspenger.datadeling.behandling.datadeling

import no.nav.tiltakspenger.datadeling.behandling.datadeling.routes.TpsakBehandlingRespons
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.domene.Behandling
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

class BehandlingService(
    private val behandlingRepo: BehandlingRepo,
) {
    fun hentBehandlingerForTp(
        fnr: Fnr,
        periode: Periode,
    ): List<Behandling> {
        return behandlingRepo.hentForFnrOgPeriode(fnr, periode)
            .filter { it.erApenSoknadsbehandling() }
            .map {
                Behandling(
                    behandlingId = it.behandlingId,
                    periode = it.periode!!,
                )
            }
    }

    fun hentApneBehandlinger(
        fnr: Fnr,
    ): List<TpsakBehandlingRespons> {
        return behandlingRepo.hentApneBehandlinger(fnr)
            .map { it.toTpsakBehandlingRespons() }
    }

    private fun TiltakspengerBehandling.erApenSoknadsbehandling() =
        this.erApenBehandling() && periode != null &&
            behandlingstype == TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING

    private fun TiltakspengerBehandling.toTpsakBehandlingRespons() =
        TpsakBehandlingRespons(
            behandlingId = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fom = periode?.fraOgMed,
            tom = periode?.tilOgMed,
            behandlingstatus = TpsakBehandlingRespons.Behandlingsstatus.valueOf(behandlingStatus.name),
            behandlingstype = TpsakBehandlingRespons.Behandlingstype.valueOf(behandlingstype.name),
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            iverksattTidspunkt = iverksattTidspunkt,
            opprettet = opprettetTidspunktSaksbehandlingApi,
            sistEndret = sistEndret,
        )
}
