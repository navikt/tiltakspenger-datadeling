package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.ArenaUtbetalingshistorikkService
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakReqDTO
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.systembruker

fun Route.arenaUtbetalingshistorikkRoutes(arenaUtbetalingshistorikkService: ArenaUtbetalingshistorikkService) {
    val logger = KotlinLogging.logger {}

    // Konsumenter per juli 2026 (se doc/konsumenter.md): NKS/Salesforce via saas-proxy.
    post("/arena/utbetalingshistorikk") {
        logger.debug { "Mottatt POST kall på /arena/utbetalingshistorikk - hent utbetalingshistorikk fra arena for fnr og periode" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /arena/utbetalingshistorikk - hent utbetalingshistorikk for fnr og periode - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseMeldekort()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /arena/utbetalingshistorikk. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_MELDEKORT}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_MELDEKORT}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /arena/utbetalingshistorikk. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { request ->
                    arenaUtbetalingshistorikkService.hentUtbetalingshistorikk(
                        fnr = request.ident,
                        periode = Periode(request.fom, request.tom),
                    ).fold(
                        // Feilen er allerede logget i servicen via HttpKlientError.loggFeil.
                        ifLeft = {
                            call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil")
                        },
                        ifRight = { utbetalingshistorikk ->
                            logger.debug { "OK /arena/utbetalingshistorikk - Systembruker ${systembruker.klientnavn}" }
                            call.respond(utbetalingshistorikk.toArenaUtbetalingshistorikkResponse())
                        },
                    )
                },
            )
    }

    // Ingen kjent konsument per juli 2026 — stien er ikke i saas-proxy-whitelisten (se doc/konsumenter.md).
    get("/arena/utbetalingshistorikk/detaljer") {
        logger.debug { "Mottatt GET kall på /arena/utbetalingshistorikk/detaljer - hent utbetalingshistorikkdetaljer fra arena for meldekortId og vedtakId" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@get
        logger.debug { "Mottatt GET kall på /arena/utbetalingshistorikk/detaljer - hent meldeperioder og meldekort for fnr og periode - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseMeldekort()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /arena/utbetalingshistorikk/detaljer. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_MELDEKORT}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_MELDEKORT}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@get
        }

        val vedtakId = call.request.queryParameters["vedtakId"]?.toLongOrNull()
        val meldekortId = call.request.queryParameters["meldekortId"]?.toLongOrNull()

        arenaUtbetalingshistorikkService.hentUtbetalingshistorikkDetaljer(
            meldekortId = meldekortId,
            vedtakId = vedtakId,
        ).fold(
            // Feilen er allerede logget i servicen via HttpKlientError.loggFeil.
            ifLeft = {
                call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil")
            },
            ifRight = { detaljer ->
                logger.debug { "OK /arena/utbetalingshistorikk/detaljer - Systembruker ${systembruker.klientnavn}" }
                call.respond(detaljer.toArenaUtbetalingshistorikkDetaljerResponse())
            },
        )
    }
}
