package no.nav.tiltakspenger.datadeling.behandling.datadeling.routes

import no.nav.tiltakspenger.datadeling.behandling.domene.Behandling
import java.time.LocalDate

/**
 * Kontrakt for åpne søknadsbehandlinger (filtrerer bort avsluttede behandlinger).
 */
data class BehandlingResponse(
    val behandlingId: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

internal fun List<Behandling>.toResponse(): List<BehandlingResponse> {
    return this.map { it.toBehandlingResponse() }
}

internal fun Behandling.toBehandlingResponse() =
    BehandlingResponse(
        behandlingId = this.behandlingId,
        fom = this.periode.fraOgMed,
        tom = this.periode.tilOgMed,
    )
