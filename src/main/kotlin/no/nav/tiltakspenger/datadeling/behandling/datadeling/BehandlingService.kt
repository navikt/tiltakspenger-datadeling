package no.nav.tiltakspenger.datadeling.behandling.datadeling

import no.nav.tiltakspenger.datadeling.behandling.datadeling.routes.TpsakBehandling
import no.nav.tiltakspenger.datadeling.behandling.datadeling.routes.TpsakBehandlingRespons
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.domene.Behandling
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengeBehandlingMedSak
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.sak.dto.toSakDTO
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class BehandlingService(
    private val behandlingRepo: BehandlingRepo,
) {
    fun hentBehandlingerForTp(
        fnr: Fnr,
        periode: Periode,
    ): List<Behandling> {
        return behandlingRepo.hentForFnrOgPeriode(fnr, periode)
            .filter { it.behandling.erApenSoknadsbehandling() }
            .map {
                Behandling(
                    behandlingId = it.behandling.behandlingId,
                    periode = it.behandling.periode!!,
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

    private fun toTpsakBehandlingRespons(behandlinger: List<TiltakspengeBehandlingMedSak>): TpsakBehandlingRespons {
        if (behandlinger.isEmpty()) {
            return TpsakBehandlingRespons(
                behandlinger = emptyList(),
                sak = null,
            )
        }
        return TpsakBehandlingRespons(
            behandlinger = behandlinger.map { it.behandling.toTpsakBehandling() }
                .sortedByDescending { it.opprettet },
            sak = behandlinger.first().sak.toSakDTO(),
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
