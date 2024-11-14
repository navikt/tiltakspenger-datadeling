package no.nav.tiltakspenger.datadeling.routes

import arrow.core.getOrElse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.service.BehandlingService
import no.nav.tiltakspenger.datadeling.service.KanIkkeHenteBehandlinger
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periodisering.Periode

private val LOG = KotlinLogging.logger {}

internal const val behandlingPath = "/behandlinger"

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    tokenService: TokenService,
) {
    post("$behandlingPath/perioder") {
        LOG.info { "Mottatt kall på hent perioder for behandlinger" }
        call.withSystembruker(tokenService) { systembruker: Systembruker ->
            call.receive<VedtakReqDTO>().toVedtakRequest()
                .fold(
                    { call.respond(HttpStatusCode.BadRequest, it) },
                    {
                        try {
                            val jsonPayload: String = behandlingService.hentBehandlingerForTp(
                                fnr = Fnr.fromString(it.ident),
                                periode = Periode(it.fom, it.tom),
                                systembruker = systembruker,
                            ).getOrElse { error ->
                                when (error) {
                                    is KanIkkeHenteBehandlinger.HarIkkeTilgang -> call.respond403Forbidden(
                                        "Mangler rollen ${error.kreverEnAvRollene}. Har rollene: ${error.harRollene}",
                                        "mangler_rolle",
                                    )
                                }
                                return@withSystembruker
                            }.toJson()
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
                    },
                )
        }
    }
}
