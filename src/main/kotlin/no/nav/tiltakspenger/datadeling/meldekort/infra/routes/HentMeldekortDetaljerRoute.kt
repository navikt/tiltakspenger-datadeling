package no.nav.tiltakspenger.datadeling.meldekort.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.auth.medSystembruker
import no.nav.tiltakspenger.datadeling.infra.routes.VedtakReqDTO
import no.nav.tiltakspenger.datadeling.meldekort.HentMeldekortService

/**
 * Konsumenter per juli 2026 (se doc/konsumenter.md): NKS/Salesforce via saas-proxy.
 *
 * Response-DTO: [MeldekortResponse]
 */
internal fun Route.hentMeldekortDetaljerRoute(
    hentMeldekortService: HentMeldekortService,
) {
    val logger = KotlinLogging.logger {}

    post("/meldekort/detaljer") {
        medSystembruker(Systembrukerrolle.LES_MELDEKORT) { systembruker ->
            call.receive<VedtakReqDTO>().toFnrOgPeriode().fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /meldekort/detaljer. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { request ->
                    val meldekortoversikt = hentMeldekortService.hentMeldekort(
                        fnr = request.fnr,
                        periode = request.periode,
                    )
                    call.respond(meldekortoversikt.toMeldekortResponse())
                },
            )
        }
    }
}
