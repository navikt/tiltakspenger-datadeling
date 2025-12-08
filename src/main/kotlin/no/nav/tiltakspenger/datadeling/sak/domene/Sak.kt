package no.nav.tiltakspenger.datadeling.sak.domene

import no.nav.tiltakspenger.libs.common.Fnr
import java.time.LocalDateTime

data class Sak(
    val id: String,
    val fnr: Fnr,
    val saksnummer: String,
    val opprettet: LocalDateTime,
)
