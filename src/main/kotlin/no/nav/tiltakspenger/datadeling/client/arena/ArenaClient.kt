package no.nav.tiltakspenger.datadeling.client.arena

import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

interface ArenaClient {
    suspend fun hent(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak>
}
