package no.nav.tiltakspenger.datadeling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.sak.SakRepo
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.texas.systembruker

internal fun Route.mottaSakRoute(
    sakRepo: SakRepo,
) {
    val log = KotlinLogging.logger {}
    post("/sak") {
        log.debug { "Mottatt POST kall på /sak - lagre sak fra tiltakspenger-saksbehandling-api" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        if (!systembruker.roller.kanLagreTiltakspengerHendelser()) {
            log.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /sak. Underliggende feil: Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }

        call.withBody<MottaSakRequest> { body ->
            try {
                sakRepo.lagre(body.toDomain())
                call.respond(HttpStatusCode.OK)
                log.debug { "Systembruker ${systembruker.klientnavn} lagret sak OK." }
            } catch (e: Exception) {
                log.error { "Systembruker ${systembruker.klientnavn} fikk 500 Internal Server Error mot POST /sak. Underliggende feil: ${e.message}" }
                call.respond500InternalServerError(
                    "Sak kunne ikke lagres siden en ukjent feil oppstod",
                    "ukjent_feil",
                )
                return@withBody
            }
        }
    }
}
