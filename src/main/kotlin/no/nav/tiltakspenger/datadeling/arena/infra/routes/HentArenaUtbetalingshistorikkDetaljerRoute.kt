package no.nav.tiltakspenger.datadeling.arena.infra.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.arena.HentArenaUtbetalingshistorikkDetaljerService
import no.nav.tiltakspenger.datadeling.infra.auth.medSystembruker
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError

/**
 * Ingen kjent konsument per juli 2026 — stien er ikke i saas-proxy-whitelisten (se doc/konsumenter.md).
 *
 * Response-DTO: [ArenaUtbetalingshistorikkDetaljerResponse]
 */
internal fun Route.hentArenaUtbetalingshistorikkDetaljerRoute(
    hentArenaUtbetalingshistorikkDetaljerService: HentArenaUtbetalingshistorikkDetaljerService,
) {
    get("/arena/utbetalingshistorikk/detaljer") {
        medSystembruker(Systembrukerrolle.LES_MELDEKORT) {
            // Arena slår opp på den av id-ene som er satt; ugyldige verdier blir null og håndteres av Arena.
            val vedtakId = call.request.queryParameters["vedtakId"]?.toLongOrNull()
            val meldekortId = call.request.queryParameters["meldekortId"]?.toLongOrNull()

            hentArenaUtbetalingshistorikkDetaljerService.hentArenaUtbetalingshistorikkDetaljer(
                meldekortId = meldekortId,
                vedtakId = vedtakId,
            ).fold(
                // Feilen er allerede logget i servicen via HttpKlientError.loggFeil.
                ifLeft = {
                    call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil")
                },
                ifRight = { detaljer ->
                    call.respond(detaljer.toArenaUtbetalingshistorikkDetaljerResponse())
                },
            )
        }
    }
}
