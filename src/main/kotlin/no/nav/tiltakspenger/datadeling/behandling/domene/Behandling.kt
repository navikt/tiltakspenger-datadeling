package no.nav.tiltakspenger.datadeling.behandling.domene

import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Det er implisitt av behandlingen er åpen, dvs. ikke avsluttet.
 * Avsluttet kan være avbrutt eller iverksatt.
 */
data class Behandling(
    val behandlingId: String,
    val periode: Periode,
)
