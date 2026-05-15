package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.datadeling.sak.Sak

data class TiltakspengeVedtakMedSak(
    val sak: Sak,
    val vedtak: TiltakspengerVedtak,
)
