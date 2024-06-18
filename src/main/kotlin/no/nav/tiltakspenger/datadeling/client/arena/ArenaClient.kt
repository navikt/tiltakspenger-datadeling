package no.nav.tiltakspenger.datadeling.client.arena

import no.nav.tiltakspenger.datadeling.domene.Periode
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

interface ArenaClient {
    suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak>
    suspend fun hentPerioder(ident: String, fom: LocalDate, tom: LocalDate): List<Periode>
}
