package no.nav.tiltakspenger.datadeling.vedtak.domene

import no.nav.tiltakspenger.datadeling.sak.domene.Sak

data class TiltakspengeVedtakMedSak(
    val sak: Sak,
    val vedtak: TiltakspengerVedtak,
)
