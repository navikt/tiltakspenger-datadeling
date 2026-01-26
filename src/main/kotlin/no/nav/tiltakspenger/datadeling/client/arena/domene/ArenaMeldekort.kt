package no.nav.tiltakspenger.datadeling.client.arena.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class ArenaMeldekort(
    val meldekortId: String,
    val mottatt: LocalDate?,
    val arbeidet: Boolean,
    val kurs: Boolean,
    val ferie: Boolean?,
    val syk: Boolean,
    val annetFravaer: Boolean,
    val fortsattArbeidsoker: Boolean,
    val registrert: LocalDateTime,
    val sistEndret: LocalDateTime,
    val type: String,
    val status: String,
    val statusDato: LocalDate,
    val meldegruppe: String,
    val aar: Int,
    val totaltArbeidetTimer: Int,
    val periode: ArenaMeldekortPeriode,
    val dager: List<ArenaMeldekortDag>,
) {
    data class ArenaMeldekortPeriode(
        val aar: Int,
        val periodekode: Int,
        val ukenrUke1: Int,
        val ukenrUke2: Int,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )

    class ArenaMeldekortDag(
        val ukeNr: Int,
        val dagNr: Int,
        val arbeidsdag: Boolean,
        val ferie: Boolean?,
        val kurs: Boolean,
        val syk: Boolean,
        val annetFravaer: Boolean,
        val registrertAv: String,
        val registrert: LocalDateTime,
        val arbeidetTimer: Int,
    )
}
