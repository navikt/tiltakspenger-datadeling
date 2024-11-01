package no.nav.tiltakspenger.datadeling.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.Configuration.applicationProfile
import no.nav.tiltakspenger.datadeling.Profile
import no.nav.tiltakspenger.datadeling.domene.Behandling
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
                    // Samtidighetskontroll prodsettes 02.10.24
                    // Vi har ikke noe data i prod, så vi svarer med tom liste i først omgang
                    // Trellokort med beskrivelser https://trello.com/c/5Q9Cag7x/1093-legge-til-rette-for-prodsetting-av-samtidighetskontroll-i-arena
                    // TODO pre-mvp jah: Gi tom liste mens vi prøver å endre tiltakspenger-vedtak -> tiltakspenger-saksbehandling-api
                    if (applicationProfile() == Profile.PROD || applicationProfile() == Profile.DEV) {
                        call.respond(HttpStatusCode.OK, emptyList<Behandling>())
                    } else if (applicationProfile() == Profile.DEV || applicationProfile() == Profile.LOCAL) {
                        try {
                            val jsonPayload: String = behandlingService.hentBehandlinger(
                                ident = it.ident,
                                fom = it.fom,
                                tom = it.tom,
                            ).toJson()
                            call.respondText(
                                status = HttpStatusCode.OK,
                                text = jsonPayload,
                                contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
                            )
                        } catch (e: Exception) {
                            call.respond(
                                status = HttpStatusCode.InternalServerError,
                                message = InternalError(feilmelding = e.message ?: "Ukjent feil"),
                            )
                        }
                    }
                },
            )
    }
}
