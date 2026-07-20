package no.nav.tiltakspenger.datadeling.testdata

import no.nav.tiltakspenger.datadeling.sak.Sak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import java.time.LocalDateTime

object SakMother {
    fun sak(
        id: String = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
        saksnummer: String = "202401011001",
        fnr: Fnr = Fnr.fromString("12345678901"),
        opprettet: LocalDateTime = nå(fixedClock),
    ): Sak = Sak(
        id = SakId.fromString(id),
        fnr = fnr,
        saksnummer = Saksnummer(saksnummer),
        opprettet = opprettet,
    )
}
