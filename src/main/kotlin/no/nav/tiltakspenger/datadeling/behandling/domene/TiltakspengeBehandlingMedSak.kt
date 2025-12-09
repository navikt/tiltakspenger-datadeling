package no.nav.tiltakspenger.datadeling.behandling.domene

import no.nav.tiltakspenger.datadeling.sak.domene.Sak

data class TiltakspengeBehandlingMedSak(
    val sak: Sak,
    val behandling: TiltakspengerBehandling,
)
