package no.nav.tiltakspenger.datadeling.routes

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
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
import no.nav.tiltakspenger.datadeling.service.KanIkkeHenteVedtak
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

private val LOG = KotlinLogging.logger {}

internal const val vedtakPath = "/vedtak"

fun Route.vedtakRoutes(
    vedtakService: VedtakService,
    tokenService: TokenService,
) {
    post("$vedtakPath/detaljer") {
        LOG.info { "Mottatt kall på hent detaljer" }
        call.withSystembruker(tokenService) { systembruker: Systembruker ->
            call.receive<VedtakReqDTO>().toVedtakRequest()
                .fold(
                    { call.respond(HttpStatusCode.BadRequest, it) },
                    {
                        try {
                            val jsonPayload = vedtakService.hentTpVedtak(
                                fnr = Fnr.fromString(it.ident),
                                periode = Periode(it.fom, it.tom),
                                systembruker = systembruker,
                            ).getOrElse { error ->
                                when (error) {
                                    is KanIkkeHenteVedtak.HarIkkeTilgang -> call.respond403Forbidden(
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

    post("$vedtakPath/perioder") {
        LOG.info { "Mottatt kall på hent perioder" }
        call.withSystembruker(tokenService) { systembruker: Systembruker ->
            call.receive<VedtakReqDTO>().toVedtakRequest()
                .fold(
                    { call.respond(HttpStatusCode.BadRequest, it) },
                    {
                        try {
                            val jsonPayload: String = vedtakService.hentPerioder(
                                fnr = Fnr.fromString(it.ident),
                                periode = Periode(it.fom, it.tom),
                                systembruker = systembruker,
                            ).getOrElse { error ->
                                when (error) {
                                    is KanIkkeHenteVedtak.HarIkkeTilgang -> call.respond403Forbidden(
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

data class InternalError(
    val feilmelding: String,
)

data class MappingError(
    val feilmelding: String,
)

data class VedtakRequest(
    val ident: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class VedtakReqDTO(
    val ident: String?,
    val fom: String?,
    val tom: String?,
) {
    fun toVedtakRequest(): Either<MappingError, VedtakRequest> {
        if (ident.isNullOrBlank()) {
            return MappingError(
                feilmelding = "Mangler ident",
            ).left()
        }

        // Går veien via Fnr for å bruke felles validering av ident
        val ident = try {
            Fnr.fromString(ident)
        } catch (_: Exception) {
            return MappingError(
                feilmelding = "Ident $ident er ugyldig. Må bestå av 11 siffer",
            ).left()
        }

        val fraDato = if (fom.isNullOrBlank()) {
            LocalDate.of(1970, 1, 1)
        } else {
            try {
                LocalDate.parse(fom)
            } catch (_: Exception) {
                return MappingError(
                    feilmelding = "Ugyldig datoformat for fom-dato: $fom",
                ).left()
            }
        }

        val tilDato = if (tom.isNullOrBlank()) {
            LocalDate.of(9999, 12, 31)
        } else {
            try {
                LocalDate.parse(tom)
            } catch (_: Exception) {
                return MappingError(
                    feilmelding = "Ugyldig datoformat for tom-dato: $tom",
                ).left()
            }
        }

        if (fraDato.isAfter(tilDato)) {
            return MappingError(
                feilmelding = "Fra-dato $fraDato ikke være etter til-dato $tilDato",
            ).left()
        }

        return VedtakRequest(
            ident = ident.verdi,
            fom = fraDato,
            tom = tilDato,
        ).right()
    }
}
