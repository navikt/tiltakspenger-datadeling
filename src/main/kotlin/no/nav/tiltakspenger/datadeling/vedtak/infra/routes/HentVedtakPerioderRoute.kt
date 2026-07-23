package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.vedtak.HentVedtaksperioderService
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.systembruker

// Konsumenter per juli 2026 (se doc/konsumenter.md): behandlingsflyt (Kelvin), modiapersonoversikt-api og tilleggsstønader.
internal fun Route.hentVedtakPerioderRoute(
    hentVedtaksperioderService: HentVedtaksperioderService,
) {
    val logger = KotlinLogging.logger {}

    post("/vedtak/perioder") {
        logger.debug { "Mottatt POST kall på /vedtak/perioder - hent vedtak for fnr og periode" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /vedtak/perioder - hent vedtak for fnr og periode - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseVedtak()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /vedtak/perioder. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_VEDTAK}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_VEDTAK}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                {
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /vedtak/perioder. Underliggende feil: $it" }
                    call.respond(HttpStatusCode.BadRequest, it)
                },
                { request ->
                    hentVedtaksperioderService.hentVedtaksperioder(
                        fnr = request.ident,
                        periode = Periode(request.fom, request.tom),
                    ).fold(
                        // Feilen er allerede logget i servicen via HttpKlientError.loggFeil.
                        ifLeft = {
                            call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil")
                        },
                        ifRight = { vedtak ->
                            logger.debug { "OK /vedtak/perioder - Systembruker ${systembruker.klientnavn}" }
                            call.respond(vedtak.toVedtakDTO())
                        },
                    )
                },
            )
    }
}
