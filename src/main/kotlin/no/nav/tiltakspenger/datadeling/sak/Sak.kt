package no.nav.tiltakspenger.datadeling.sak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import java.time.LocalDateTime

data class Sak(
    val id: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val opprettet: LocalDateTime,
)

data class MottaSakKommando(
    val nySak: NySak,
)

data class NySak(
    val id: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val opprettet: LocalDateTime,
) {
    fun tilSak(): Sak = Sak(
        id = id,
        fnr = fnr,
        saksnummer = saksnummer,
        opprettet = opprettet,
    )
}
