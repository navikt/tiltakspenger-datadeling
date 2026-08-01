package no.nav.tiltakspenger.datadeling.behandling.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.behandling.HentBehandlingerForPeriodeService
import no.nav.tiltakspenger.datadeling.infra.auth.medSystembruker
import no.nav.tiltakspenger.datadeling.infra.routes.VedtakReqDTO

/**
 * Returnerer åpne søknadsbehandlinger som overlapper med angitt periode.
 * Ingen kjent konsument per juli 2026, muligens arena (se doc/konsumenter.md).
 *
 * Response-DTO: [BehandlingResponse]
 */
// TODO jah: Gi POST /behandlinger/perioder en egen BehandlingPerioderRequestDTO i denne route-filen i stedet for å gjenbruke den delte VedtakReqDTO.
// TODO jah: Avklar om manglende/blank fom/tom skal bety åpent intervall, eller om behandling/perioder skal kreve eksplisitt periode.
fun Route.hentBehandlingerForPeriodeRoute(
    hentBehandlingerForPeriodeService: HentBehandlingerForPeriodeService,
) {
    val logger = KotlinLogging.logger {}

    post("/behandlinger/perioder") {
        medSystembruker(Systembrukerrolle.LES_BEHANDLING) { systembruker ->
            call.receive<VedtakReqDTO>().toFnrOgPeriode().fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /behandlinger/perioder. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { request ->
                    val behandlinger = hentBehandlingerForPeriodeService.hentBehandlingerForPeriode(
                        fnr = request.fnr,
                        periode = request.periode,
                    ).toResponse()
                    call.respond(behandlinger)
                },
            )
        }
    }
}
