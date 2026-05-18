package no.nav.tiltakspenger.datadeling.behandling.infra.routes

import no.nav.tiltakspenger.datadeling.behandling.TiltakspengeBehandlingMedSak
import no.nav.tiltakspenger.datadeling.behandling.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.sak.Sak
import java.time.LocalDate
import java.time.LocalDateTime

data class TpsakBehandlingResponseDTO(
    val behandlinger: List<BehandlingDTO>,
    val sak: SakDTO?,
) {
    data class SakDTO(
        val sakId: String,
        val saksnummer: String,
        val kilde: String,
        val status: String,
        val opprettetDato: LocalDateTime,
    )

    data class BehandlingDTO(
        val behandlingId: String,
        val fom: LocalDate?,
        val tom: LocalDate?,
        val behandlingstatus: Behandlingsstatus,
        val behandlingstype: Behandlingstype,
        val saksbehandler: String?,
        val beslutter: String?,
        val iverksattTidspunkt: LocalDateTime?,
        val opprettet: LocalDateTime,
        val sistEndret: LocalDateTime,
    ) {
        enum class Behandlingsstatus {
            UNDER_AUTOMATISK_BEHANDLING,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            VEDTATT,
            AVBRUTT,
            GODKJENT,
            AUTOMATISK_BEHANDLET,
            IKKE_RETT_TIL_TILTAKSPENGER,
        }

        enum class Behandlingstype {
            SOKNADSBEHANDLING,
            REVURDERING,
            MELDEKORTBEHANDLING,
        }
    }
}

internal fun List<TiltakspengeBehandlingMedSak>.tilTpsakBehandlingResponseDTO(): TpsakBehandlingResponseDTO {
    if (isEmpty()) {
        return TpsakBehandlingResponseDTO(
            behandlinger = emptyList(),
            sak = null,
        )
    }
    return TpsakBehandlingResponseDTO(
        behandlinger = map { tilTpsakBehandlingDTO(it.behandling) },
        sak = tilTpsakBehandlingSakDTO(first().sak),
    )
}

private fun tilTpsakBehandlingDTO(behandling: TiltakspengerBehandling) =
    TpsakBehandlingResponseDTO.BehandlingDTO(
        behandlingId = behandling.behandlingId,
        fom = behandling.periode?.fraOgMed,
        tom = behandling.periode?.tilOgMed,
        behandlingstatus = TpsakBehandlingResponseDTO.BehandlingDTO.Behandlingsstatus.valueOf(
            behandling.behandlingStatus.name,
        ),
        behandlingstype = TpsakBehandlingResponseDTO.BehandlingDTO.Behandlingstype.valueOf(
            behandling.behandlingstype.name,
        ),
        saksbehandler = behandling.saksbehandler,
        beslutter = behandling.beslutter,
        iverksattTidspunkt = behandling.iverksattTidspunkt,
        opprettet = behandling.opprettetTidspunktSaksbehandlingApi,
        sistEndret = behandling.sistEndret,
    )

private fun tilTpsakBehandlingSakDTO(sak: Sak): TpsakBehandlingResponseDTO.SakDTO =
    TpsakBehandlingResponseDTO.SakDTO(
        sakId = sak.id.toString(),
        saksnummer = sak.saksnummer.verdi,
        kilde = "TPSAK",
        status = "Løpende",
        opprettetDato = sak.opprettet,
    )
