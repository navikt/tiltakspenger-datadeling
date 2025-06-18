package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDateTime

/**
 * Domenemodell for tiltakspenger. Arena vil få sin egen modell.
 * På sikt vil den generelle Behandling være et interface som dekker begge.
 */
data class TiltakspengerBehandling(
    val behandlingId: String,
    val sakId: String,
    val periode: Periode,
    val behandlingStatus: Behandlingsstatus,
    val saksbehandler: String?,
    val beslutter: String?,
    val iverksattTidspunkt: LocalDateTime?,
    val fnr: Fnr,
    val saksnummer: String,
    val søknadJournalpostId: String,
    val opprettetTidspunktSaksbehandlingApi: LocalDateTime,
    val mottattTidspunktDatadeling: LocalDateTime,
) {
    val kilde = Kilde.TPSAK

    enum class Behandlingsstatus {
        UNDER_AUTOMATISK_BEHANDLING,
        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        UNDER_BESLUTNING,
        VEDTATT,
        AVBRUTT,
    }
}
