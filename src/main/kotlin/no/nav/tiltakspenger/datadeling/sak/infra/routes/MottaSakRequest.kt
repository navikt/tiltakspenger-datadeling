package no.nav.tiltakspenger.datadeling.sak.infra.routes

import no.nav.tiltakspenger.datadeling.sak.Sak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import java.time.LocalDateTime

/**
 * Request-DTO for `POST /sak`. Internt endepunkt som
 * tiltakspenger-saksbehandling-api bruker for å speile saker hit – derfor
 * ikke dokumentert i den eksterne openapi-specen.
 */
data class MottaSakRequest(
    val id: String,
    val fnr: String,
    val saksnummer: String,
    val opprettet: LocalDateTime,
) {
    fun toDomain(): Sak = Sak(
        id = SakId.fromString(id),
        fnr = Fnr.fromString(fnr),
        saksnummer = Saksnummer(saksnummer),
        opprettet = opprettet,
    )
}
