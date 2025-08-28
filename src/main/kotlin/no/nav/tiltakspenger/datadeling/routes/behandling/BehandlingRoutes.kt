package no.nav.tiltakspenger.datadeling.routes.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.routes.vedtak.VedtakReqDTO
import no.nav.tiltakspenger.datadeling.service.BehandlingService
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.texas.systembruker

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
) {
    val logger = KotlinLogging.logger {}
    // Brukes av arena
    post("/behandlinger/perioder") {
        logger.debug { "Mottatt POST kall på /behandlinger/perioder - hent behandlinger for periode og fnr" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /behandlinger/perioder - hent behandlinger for periode og fnr - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseBehandlinger()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /behandlinger/perioder. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_BEHANDLING}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_BEHANDLING}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                {
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /behandlinger/perioder. Underliggende feil: $it" }
                    call.respond(HttpStatusCode.BadRequest, it)
                },
                {
                    val behandlinger = behandlingService.hentBehandlingerForTp(
                        fnr = it.ident,
                        periode = Periode(it.fom, it.tom),
                    ).toResponse()
                    logger.debug { "OK /behandlinger/perioder - Systembruker ${systembruker.klientnavn}" }
                    call.respond(behandlinger)
                },
            )
    }
}
