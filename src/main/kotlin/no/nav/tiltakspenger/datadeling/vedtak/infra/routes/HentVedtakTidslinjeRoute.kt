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
import no.nav.tiltakspenger.datadeling.vedtak.HentTidslinjeOgAlleVedtakService
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.systembruker

// Konsumenter per juli 2026 (se doc/konsumenter.md): NKS/Salesforce via saas-proxy, muligens arena.
internal fun Route.hentVedtakTidslinjeRoute(
    hentTidslinjeOgAlleVedtakService: HentTidslinjeOgAlleVedtakService,
) {
    val logger = KotlinLogging.logger {}

    post("/vedtak/tidslinje") {
        logger.debug { "Mottatt POST kall på /vedtak/tidslinje - hent vedtak og tidslinje for fnr og periode" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /vedtak/tidslinje - hent vedtak og tidslinje for fnr og periode - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseVedtak()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /vedtak/tidslinje. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_VEDTAK}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_VEDTAK}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /vedtak/tidslinje. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { request ->
                    hentTidslinjeOgAlleVedtakService.hentTidslinjeOgAlleVedtak(
                        fnr = request.ident,
                        periode = Periode(request.fom, request.tom),
                    ).fold(
                        // Feilen er allerede logget i servicen via HttpKlientError.loggFeil.
                        ifLeft = {
                            call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil")
                        },
                        ifRight = { vedtakTidslinje ->
                            logger.debug { "OK /vedtak/tidslinje - Systembruker ${systembruker.klientnavn}" }
                            call.respond(vedtakTidslinje.toVedtakTidslinjeResponse())
                        },
                    )
                },
            )
    }
}
