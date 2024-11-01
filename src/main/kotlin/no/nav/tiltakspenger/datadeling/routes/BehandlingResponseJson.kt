package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.felles.infra.json.serialize

/**
 * Åpne behandlinger (filtrerer bort avsluttede behandlinger: iverksatt+avbrutt)
 */
private data class BehandlingResponseJson(
    val behandlingId: String,
    val fom: String,
    val tom: String,
)

internal fun List<Behandling>.toJson(): String {
    return this.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toJson() }
}
internal fun Behandling.toJson(): String {
    return serialize(
        BehandlingResponseJson(
            behandlingId = this.behandlingId,
            fom = this.fom.toString(),
            tom = this.tom.toString(),
        ),
    )
}
