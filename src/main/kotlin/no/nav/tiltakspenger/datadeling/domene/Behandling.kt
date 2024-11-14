package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode

/**
 * Det er implisitt av behandlingen er åpen, dvs. ikke avsluttet.
 * Avsluttet kan være avbrutt eller iverksatt.
 */
data class Behandling(
    val behandlingId: String,
    val periode: Periode,
)
