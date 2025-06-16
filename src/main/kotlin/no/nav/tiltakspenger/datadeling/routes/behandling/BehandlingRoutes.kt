package no.nav.tiltakspenger.datadeling.routes.behandling

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.routes.vedtak.VedtakReqDTO
import no.nav.tiltakspenger.datadeling.service.BehandlingService
import no.nav.tiltakspenger.datadeling.service.KanIkkeHenteBehandlinger
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periodisering.Periode

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}
    // Brukes av arena
    post("/behandlinger/perioder") {
        logger.debug { "Mottatt POST kall på /behandlinger/perioder - hent behandlinger for periode og fnr" }
        call.withSystembruker(tokenService) { systembruker: Systembruker ->
            logger.debug { "Mottatt POST kall på /behandlinger/perioder - hent behandlinger for periode og fnr - systembruker $systembruker" }
            call.receive<VedtakReqDTO>().toVedtakRequest()
                .fold(
                    {
                        logger.error { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /behandlinger/perioder. Underliggende feil: $it" }
                        call.respond(HttpStatusCode.BadRequest, it)
                    },
                    {
                        val behandlinger = behandlingService.hentBehandlingerForTp(
                            fnr = it.ident,
                            periode = Periode(it.fom, it.tom),
                            systembruker = systembruker,
                        ).getOrElse { error ->
                            when (error) {
                                is KanIkkeHenteBehandlinger.HarIkkeTilgang -> {
                                    logger.error { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /behandlinger/perioder. Underliggende feil: $error" }
                                    call.respond403Forbidden(
                                        "Mangler rollen ${error.kreverEnAvRollene}. Har rollene: ${error.harRollene}",
                                        "mangler_rolle",
                                    )
                                }
                            }
                            return@withSystembruker
                        }.toResponse()
                        logger.debug { "OK /behandlinger/perioder - Systembruker ${systembruker.klientnavn}" }
                        call.respond(behandlinger)
                    },
                )
        }
    }
}
