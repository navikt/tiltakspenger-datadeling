package no.nav.tiltakspenger.datadeling.behandling.infra.routes

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.behandling.HentApneBehandlingerService
import no.nav.tiltakspenger.datadeling.infra.auth.medSystembruker
import no.nav.tiltakspenger.datadeling.infra.routes.MappingError
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Returnerer alle åpne behandlinger for en person, med saken de hører til.
 * Konsumenter per juli 2026 (se doc/konsumenter.md): NKS/Salesforce via saas-proxy, muligens arena.
 *
 * Response-DTO: [TpsakBehandlingResponseDTO]
 */
internal fun Route.hentApneBehandlingerRoute(
    hentApneBehandlingerService: HentApneBehandlingerService,
) {
    val logger = KotlinLogging.logger {}

    post("/behandlinger/apne") {
        medSystembruker(Systembrukerrolle.LES_BEHANDLING) { systembruker ->
            call.receive<BehandlingRequestDTO>().toFnr().fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /behandlinger/apne. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { fnr ->
                    val behandlinger = hentApneBehandlingerService.hentApneBehandlinger(fnr).tilTpsakBehandlingResponseDTO()
                    call.respond(behandlinger)
                },
            )
        }
    }
}

private data class BehandlingRequestDTO(
    val ident: String,
) {
    fun toFnr(): Either<MappingError, Fnr> {
        val fnr = try {
            Fnr.fromString(ident)
        } catch (_: Exception) {
            return MappingError(
                feilmelding = "Ident $ident er ugyldig. Må bestå av 11 siffer",
            ).left()
        }
        return fnr.right()
    }
}
