package no.nav.tiltakspenger.datadeling.meldekort.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldeperiode(
    val id: MeldeperiodeId,
    val kjedeId: String,
    val sakId: SakId,
    val opprettet: LocalDateTime,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val maksAntallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
) {
    val minstEnDagGirRettIPerioden = girRett.any { it.value }
    val kanFyllesUtFraOgMed: LocalDate = tilOgMed.minusDays(2)
    val erKlarTilUtfylling = !LocalDate.now().isBefore(kanFyllesUtFraOgMed)
}
