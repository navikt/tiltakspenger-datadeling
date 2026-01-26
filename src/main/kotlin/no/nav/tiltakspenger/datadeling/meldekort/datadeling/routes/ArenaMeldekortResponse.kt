package no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes

import java.time.LocalDate
import java.time.LocalDateTime

data class ArenaMeldekortResponse(
    val meldekortId: String,
    val mottatt: LocalDate?,
    val arbeidet: Boolean,
    val kurs: Boolean,
    val ferie: Boolean?,
    val syk: Boolean,
    val annetFravaer: Boolean,
    val registrert: LocalDateTime,
    val sistEndret: LocalDateTime,
    val type: String,
    val status: String,
    val statusDato: LocalDate,
    val meldegruppe: String,
    val aar: Int,
    val totaltArbeidetTimer: Int,
    val periode: ArenaMeldekortPeriodeResponse,
    val dager: List<ArenaMeldekortDagResponse>,
    val fortsattArbeidsoker: Boolean,
) {
    data class ArenaMeldekortPeriodeResponse(
        val aar: Int,
        val periodekode: Int,
        val ukenrUke1: Int,
        val ukenrUke2: Int,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )

    class ArenaMeldekortDagResponse(
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
