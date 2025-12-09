package no.nav.tiltakspenger.datadeling.behandling.datadeling.routes

import no.nav.tiltakspenger.datadeling.sak.dto.SakDTO
import java.time.LocalDate
import java.time.LocalDateTime

data class TpsakBehandlingRespons(
    val behandlinger: List<TpsakBehandling>,
    val sak: SakDTO?,
)

data class TpsakBehandling(
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
