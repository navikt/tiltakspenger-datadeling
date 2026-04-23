package no.nav.tiltakspenger.datadeling.vedtak.domene

import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.TILTAKSPENGER
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
import java.time.LocalDateTime

data class TiltakspengeSakMedVedtak(
    val sak: Sak,
    val vedtak: List<TiltakspengerVedtak>,
) {
    val iverksattSøknadsbehandlingTidspunkt: LocalDateTime? by lazy {
        vedtak.sortedBy { it.opprettet }
            .firstOrNull { it.rettighet in listOf(TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG) }
            ?.opprettet
    }
}
