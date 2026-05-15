package no.nav.tiltakspenger.datadeling.behandling

import no.nav.tiltakspenger.datadeling.sak.Sak

data class TiltakspengeBehandlingMedSak(
    val sak: Sak,
    val behandling: TiltakspengerBehandling,
)
