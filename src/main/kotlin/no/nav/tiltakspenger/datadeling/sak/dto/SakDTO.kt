package no.nav.tiltakspenger.datadeling.sak.dto

import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import java.time.LocalDateTime

data class SakDTO(
    val sakId: String,
    val saksnummer: String,
    val kilde: String = "TPSAK",
    val status: String = "Løpende",
    val opprettetDato: LocalDateTime,
)

fun Sak.toSakDTO() = SakDTO(
    sakId = id.toString(),
    saksnummer = saksnummer.verdi,
    opprettetDato = opprettet,
)
