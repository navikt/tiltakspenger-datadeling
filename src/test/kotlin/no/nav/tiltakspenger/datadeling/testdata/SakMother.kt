package no.nav.tiltakspenger.datadeling.testdata

import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import java.time.LocalDateTime

object SakMother {
    fun sak(
        id: String = "sakId",
        saksnummer: String = "saksnummer",
        fnr: Fnr = Fnr.fromString("12345678901"),
        opprettet: LocalDateTime = LocalDateTime.now(),
        rammevedtak: List<TiltakspengerVedtak> = emptyList(),
        behandlinger: List<TiltakspengerBehandling> = emptyList(),
    ): Sak = Sak(
        id = id,
        fnr = fnr,
        saksnummer = saksnummer,
        opprettet = opprettet,
        rammevedtak = rammevedtak,
        behandlinger = behandlinger,
    )
}
