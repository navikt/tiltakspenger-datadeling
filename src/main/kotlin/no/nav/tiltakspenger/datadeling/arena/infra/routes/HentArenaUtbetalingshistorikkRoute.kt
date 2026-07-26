package no.nav.tiltakspenger.datadeling.arena.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.arena.HentArenaUtbetalingshistorikkService
import no.nav.tiltakspenger.datadeling.infra.auth.medSystembruker
import no.nav.tiltakspenger.datadeling.infra.routes.VedtakReqDTO
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError

/**
 * Konsumenter per juli 2026 (se doc/konsumenter.md): NKS/Salesforce via saas-proxy.
 *
 * Response-DTO: [ArenaUtbetalingshistorikkResponse]
 */
internal fun Route.hentArenaUtbetalingshistorikkRoute(
    hentArenaUtbetalingshistorikkService: HentArenaUtbetalingshistorikkService,
) {
    val logger = KotlinLogging.logger {}

    post("/arena/utbetalingshistorikk") {
        medSystembruker(Systembrukerrolle.LES_MELDEKORT) { systembruker ->
            call.receive<VedtakReqDTO>().toFnrOgPeriode().fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /arena/utbetalingshistorikk. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { request ->
                    hentArenaUtbetalingshistorikkService.hentArenaUtbetalingshistorikk(
                        fnr = request.fnr,
                        periode = request.periode,
                    ).fold(
                        // Feilen er allerede logget i servicen via HttpKlientError.loggFeil.
                        ifLeft = {
                            call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil")
                        },
                        ifRight = { utbetalingshistorikk ->
                            call.respond(utbetalingshistorikk.toArenaUtbetalingshistorikkResponse())
                        },
                    )
                },
            )
        }
    }
}
