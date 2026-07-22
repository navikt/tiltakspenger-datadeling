package no.nav.tiltakspenger.datadeling.meldekort.infra.routes

import no.nav.tiltakspenger.datadeling.arena.ArenaMeldekort
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

internal fun List<ArenaMeldekort>.toArenaMeldekortResponse(): List<ArenaMeldekortResponse> = map { it.toArenaMeldekortResponse() }

private fun ArenaMeldekort.toArenaMeldekortResponse(): ArenaMeldekortResponse = ArenaMeldekortResponse(
    meldekortId = meldekortId,
    mottatt = mottatt,
    arbeidet = arbeidet,
    kurs = kurs,
    ferie = ferie,
    syk = syk,
    annetFravaer = annetFravaer,
    fortsattArbeidsoker = fortsattArbeidsoker,
    registrert = registrert,
    sistEndret = sistEndret,
    type = type,
    status = status,
    statusDato = statusDato,
    meldegruppe = meldegruppe,
    aar = aar,
    totaltArbeidetTimer = totaltArbeidetTimer,
    periode = ArenaMeldekortResponse.ArenaMeldekortPeriodeResponse(
        aar = periode.aar,
        periodekode = periode.periodekode,
        ukenrUke1 = periode.ukenrUke1,
        ukenrUke2 = periode.ukenrUke2,
        fraOgMed = periode.fraOgMed,
        tilOgMed = periode.tilOgMed,
    ),
    dager = dager.map { dag ->
        ArenaMeldekortResponse.ArenaMeldekortDagResponse(
            ukeNr = dag.ukeNr,
            dagNr = dag.dagNr,
            arbeidsdag = dag.arbeidsdag,
            ferie = dag.ferie,
            kurs = dag.kurs,
            syk = dag.syk,
            annetFravaer = dag.annetFravaer,
            registrertAv = dag.registrertAv,
            registrert = dag.registrert,
            arbeidetTimer = dag.arbeidetTimer,
        )
    },
)
