package no.nav.tiltakspenger.datadeling.routes.vedtak

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.service.KanIkkeHenteVedtak
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

internal const val VEDTAK_PATH = "/vedtak"

fun Route.vedtakRoutes(
    vedtakService: VedtakService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}

    post("/vedtak/detaljer") {
        logger.debug { "Mottatt POST kall på /vedtak/detaljer - hent vedtaksdetaljer for fnr og periode" }
        call.withSystembruker(tokenService) { systembruker: Systembruker ->
            logger.debug { "Mottatt POST kall på /vedtak/detaljer - hent vedtaksdetaljer for fnr og periode - systembruker $systembruker" }
            call.receive<VedtakReqDTO>().toVedtakRequest()
                .fold(
                    { error ->
                        logger.error { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /vedtak/detaljer. Underliggende feil: $error" }
                        call.respond(HttpStatusCode.BadRequest, error)
                    },
                    {
                        val vedtak = vedtakService.hentTpVedtak(
                            fnr = it.ident,
                            periode = Periode(it.fom, it.tom),
                            systembruker = systembruker,
                        ).getOrElse { error ->
                            when (error) {
                                is KanIkkeHenteVedtak.HarIkkeTilgang -> {
                                    logger.error { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /vedtak/detaljer. Underliggende feil: $error" }
                                    call.respond403Forbidden(
                                        "Mangler rollen ${error.kreverEnAvRollene}. Har rollene: ${error.harRollene}",
                                        "mangler_rolle",
                                    )
                                }
                            }
                            return@withSystembruker
                        }.toVedtakDetaljerResponse()
                        logger.debug { "OK /vedtak/detaljer - Systembruker ${systembruker.klientnavn}" }
                        call.respond(vedtak)
                    },
                )
        }
    }

    post("/vedtak/perioder") {
        logger.debug { "Mottatt POST kall på /vedtak/perioder - hent vedtak for fnr og periode" }
        call.withSystembruker(tokenService) { systembruker: Systembruker ->
            logger.debug { "Mottatt POST kall på /vedtak/perioder - hent vedtak for fnr og periode - systembruker $systembruker" }
            call.receive<VedtakReqDTO>().toVedtakRequest()
                .fold(
                    {
                        logger.error { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /vedtak/perioder. Underliggende feil: $it" }
                        call.respond(HttpStatusCode.BadRequest, it)
                    },
                    {
                        val vedtak = vedtakService.hentVedtaksperioder(
                            fnr = it.ident,
                            periode = Periode(it.fom, it.tom),
                            systembruker = systembruker,
                        ).getOrElse { error ->
                            when (error) {
                                is KanIkkeHenteVedtak.HarIkkeTilgang -> {
                                    logger.error { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /vedtak/perioder. Underliggende feil: $error" }
                                    call.respond403Forbidden(
                                        "Mangler rollen ${error.kreverEnAvRollene}. Har rollene: ${error.harRollene}",
                                        "mangler_rolle",
                                    )
                                }
                            }
                            return@withSystembruker
                        }

                        logger.debug { "OK /vedtak/perioder - Systembruker ${systembruker.klientnavn}" }
                        call.respond(vedtak)
                    },
                )
        }
    }
}

data class MappingError(
    val feilmelding: String,
)

data class VedtakRequest(
    val ident: Fnr,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class VedtakReqDTO(
    val ident: String,
    val fom: String?,
    val tom: String?,
) {
    fun toVedtakRequest(): Either<MappingError, VedtakRequest> {
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
            ident = ident,
            fom = fraDato,
            tom = tilDato,
        ).right()
    }
}
