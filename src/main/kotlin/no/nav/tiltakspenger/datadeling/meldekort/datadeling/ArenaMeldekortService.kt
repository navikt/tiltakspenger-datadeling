package no.nav.tiltakspenger.datadeling.meldekort.datadeling

import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes.ArenaMeldekortResponse
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

class ArenaMeldekortService(
    private val arenaClient: ArenaClient,
) {
    suspend fun hentMeldekort(fnr: Fnr, periode: Periode): List<ArenaMeldekortResponse> {
        return arenaClient.hentMeldekort(
            ArenaClient.ArenaRequestDTO(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        ).map {
            ArenaMeldekortResponse(
                meldekortId = it.meldekortId,
                mottatt = it.mottatt,
                arbeidet = it.arbeidet,
                kurs = it.kurs,
                ferie = it.ferie,
                syk = it.syk,
                annetFravaer = it.annetFravaer,
                fortsattArbeidsoker = it.fortsattArbeidsoker,
                registrert = it.registrert,
                sistEndret = it.sistEndret,
                type = it.type,
                status = it.status,
                statusDato = it.statusDato,
                meldegruppe = it.meldegruppe,
                aar = it.aar,
                totaltArbeidetTimer = it.totaltArbeidetTimer,
                periode = ArenaMeldekortResponse.ArenaMeldekortPeriodeResponse(
                    aar = it.periode.aar,
                    periodekode = it.periode.periodekode,
                    ukenrUke1 = it.periode.ukenrUke1,
                    ukenrUke2 = it.periode.ukenrUke2,
                    fraOgMed = it.periode.fraOgMed,
                    tilOgMed = it.periode.tilOgMed,
                ),
                dager = it.dager.map { dag ->
                    ArenaMeldekortResponse.ArenaMeldekortDagResponse(
                        ukeNr = dag.ukeNr,
                        dagNr = dag.dagNr,
                        arbeidsdag = dag.arbeidsdag,
                        ferie = dag.ferie,
                        kurs = dag.kurs,
                        syk = dag.syk,
                        annetfravaer = dag.annetfravaer,
                        registrertAv = dag.registrertAv,
                        registrert = dag.registrert,
                        arbeidetTimer = dag.arbeidetTimer,
                    )
                },
            )
        }
    }
}
