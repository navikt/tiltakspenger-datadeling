package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.ArenaUtbetalingshistorikkService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakReqDTO
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.systembruker

fun Route.arenaUtbetalingshistorikkRoutes(arenaUtbetalingshistorikkService: ArenaUtbetalingshistorikkService) {
    val logger = KotlinLogging.logger {}

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
                {
                    val response = arenaUtbetalingshistorikkService.hentUtbetalingshistorikk(
                        fnr = it.ident,
                        periode = Periode(it.fom, it.tom),
                    )
                    logger.debug { "OK /arena/utbetalingshistorikk - Systembruker ${systembruker.klientnavn}" }
                    call.respond(response)
                },
            )
    }

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

        val response = arenaUtbetalingshistorikkService.hentUtbetalingshistorikkDetaljer(
            meldekortId = meldekortId,
            vedtakId = vedtakId,
        )
        logger.debug { "OK /arena/utbetalingshistorikk/detaljer - Systembruker ${systembruker.klientnavn}" }
        call.respond(response)
    }
}
