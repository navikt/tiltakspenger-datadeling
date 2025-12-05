package no.nav.tiltakspenger.datadeling.behandling.datadeling

import no.nav.tiltakspenger.datadeling.behandling.datadeling.routes.TpsakBehandling
import no.nav.tiltakspenger.datadeling.behandling.datadeling.routes.TpsakBehandlingRespons
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.domene.Behandling
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.domene.dto.Sak
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
    ): TpsakBehandlingRespons {
        return toTpsakBehandlingRespons(behandlingRepo.hentApneBehandlinger(fnr))
    }

    private fun TiltakspengerBehandling.erApenSoknadsbehandling() =
        this.erApenBehandling() && periode != null &&
            behandlingstype == TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING

    private fun toTpsakBehandlingRespons(behandlinger: List<TiltakspengerBehandling>): TpsakBehandlingRespons {
        if (behandlinger.isEmpty()) {
            return TpsakBehandlingRespons(
                behandlinger = emptyList(),
                sak = null,
            )
        }
        return TpsakBehandlingRespons(
            behandlinger = behandlinger.map { it.toTpsakBehandling() }
                .sortedByDescending { it.opprettet },
            sak = Sak(
                sakId = behandlinger.first().sakId,
                saksnummer = behandlinger.first().saksnummer,
            ),
        )
    }

    private fun TiltakspengerBehandling.toTpsakBehandling() =
        TpsakBehandling(
            behandlingId = behandlingId,
            fom = periode?.fraOgMed,
            tom = periode?.tilOgMed,
            behandlingstatus = TpsakBehandling.Behandlingsstatus.valueOf(behandlingStatus.name),
            behandlingstype = TpsakBehandling.Behandlingstype.valueOf(behandlingstype.name),
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            iverksattTidspunkt = iverksattTidspunkt,
            opprettet = opprettetTidspunktSaksbehandlingApi,
            sistEndret = sistEndret,
        )
}
