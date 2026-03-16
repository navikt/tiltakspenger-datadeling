package no.nav.tiltakspenger.datadeling.sak.datadeling.routes

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.sak.datadeling.SakService
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.texas.systembruker

const val SAK_PATH = "/sak"

fun Route.sakRoutes(
    sakService: SakService,
) {
    val logger = KotlinLogging.logger {}

    post(SAK_PATH) {
        logger.debug { "Mottatt POST kall på $SAK_PATH - hent sak for fnr" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på $SAK_PATH - hent sak for fnr - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseVedtak()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot $SAK_PATH. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_VEDTAK}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_VEDTAK}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<SakReqDTO>().toSakRequest()
            .fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot $SAK_PATH. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { request ->
                    val sak = sakService.hentSak(fnr = request.ident)
                    if (sak == null) {
                        logger.debug { "Fant ingen sak for bruker - Systembruker ${systembruker.klientnavn}" }
                        call.respond404NotFound("Fant ingen sak for bruker", "sak_ikke_funnet")
                        return@post
                    }
                    logger.debug { "OK $SAK_PATH - Systembruker ${systembruker.klientnavn}" }
                    call.respond(sak)
                },
            )
    }
}

data class SakMappingError(
    val feilmelding: String,
)

data class SakRequest(
    val ident: Fnr,
)

data class SakReqDTO(
    val ident: String,
) {
    fun toSakRequest(): Either<SakMappingError, SakRequest> {
        val ident = try {
            Fnr.fromString(ident)
        } catch (_: Exception) {
            return SakMappingError(
                feilmelding = "Ident $ident er ugyldig. Må bestå av 11 siffer",
            ).left()
        }

        return SakRequest(
            ident = ident,
        ).right()
    }
}
