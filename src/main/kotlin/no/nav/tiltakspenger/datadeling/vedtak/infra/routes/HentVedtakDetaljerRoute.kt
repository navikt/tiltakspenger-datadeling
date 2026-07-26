package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.auth.medSystembruker
import no.nav.tiltakspenger.datadeling.infra.routes.VedtakReqDTO
import no.nav.tiltakspenger.datadeling.vedtak.HentVedtakDetaljerService
import java.time.Clock
import java.time.LocalDate

/**
 * Konsumenter per juli 2026 (se doc/konsumenter.md): veilarbportefolje og tilleggsstønader, muligens arena.
 *
 * Response-DTO: [VedtakDetaljerResponse]
 */
internal fun Route.hentVedtakDetaljerRoute(
    hentVedtakDetaljerService: HentVedtakDetaljerService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    post("/vedtak/detaljer") {
        medSystembruker(Systembrukerrolle.LES_VEDTAK) { systembruker ->
            call.receive<VedtakReqDTO>().toFnrOgPeriode().fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /vedtak/detaljer. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { request ->
                    val vedtak = hentVedtakDetaljerService.hentVedtakDetaljer(
                        fnr = request.fnr,
                        periode = request.periode,
                    ).toVedtakDetaljerResponse(logger, LocalDate.now(clock))
                    call.respond(vedtak)
                },
            )
        }
    }
}
