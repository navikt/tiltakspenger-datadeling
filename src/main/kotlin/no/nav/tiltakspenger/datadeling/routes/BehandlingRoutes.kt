package no.nav.tiltakspenger.datadeling.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.service.BehandlingService

private val LOG = KotlinLogging.logger {}

internal const val behandlingPath = "/behandlinger"

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
) {
    post("$behandlingPath/perioder") {
        LOG.info { "Mottatt kall på hent perioder for behandlinger" }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                { call.respond(HttpStatusCode.BadRequest, it) },
                {
                    val behandlinger = behandlingService.hentBehandlinger(
                        ident = it.ident,
                        fom = it.fom,
                        tom = it.tom,
                    )
                    call.respond(status = HttpStatusCode.OK, behandlinger)
                },
            )
    }
}
