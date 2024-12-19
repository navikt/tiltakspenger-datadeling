package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
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
    val tiltaksdeltagelse: Tiltaksdeltagelse,
    val fnr: Fnr,
    val saksnummer: String,
    val søknadJournalpostId: String,
    val opprettetTidspunktSaksbehandlingApi: LocalDateTime,
    val mottattTidspunktDatadeling: LocalDateTime = nå(),
) {
    val kilde = "tp"

    enum class Behandlingsstatus {
        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        UNDER_BESLUTNING,
        VEDTATT,
    }

    /**
     * @param tiltaksnavn En beskrivelse av tiltakstypen.
     * @param eksternTiltaksdeltakerId Knytningen mellom en person og en gjennomføring. Fra Arena, Komet eller Team Tiltak.
     * @param eksternGjennomføringId Id på tiltaksgjennomføringen fra Arena, Komet eller Team Tiltak.
     */
    data class Tiltaksdeltagelse(
        val tiltaksnavn: String,
        val eksternTiltaksdeltakerId: String,
        val eksternGjennomføringId: String?,
    )
}
