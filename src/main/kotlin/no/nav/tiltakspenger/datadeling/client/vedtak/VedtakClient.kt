package no.nav.tiltakspenger.datadeling.client.vedtak

import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

interface VedtakClient {
    suspend fun hent(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak>
}
