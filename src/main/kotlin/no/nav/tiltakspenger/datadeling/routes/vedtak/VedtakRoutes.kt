package no.nav.tiltakspenger.datadeling.routes.vedtak

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.LocalDate

internal const val VEDTAK_PATH = "/vedtak"

fun Route.vedtakRoutes(
    vedtakService: VedtakService,
) {
    val logger = KotlinLogging.logger {}

    // Brukes av veilarbportefolje (OBO), saas-proxy og arena
    post("/vedtak/detaljer") {
        logger.debug { "Mottatt POST kall på /vedtak/detaljer - hent vedtaksdetaljer for fnr og periode" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /vedtak/detaljer - hent vedtaksdetaljer for fnr og periode - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseVedtak()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /vedtak/detaljer. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_VEDTAK}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_VEDTAK}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /vedtak/detaljer. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                {
                    val vedtak = vedtakService.hentTpVedtak(
                        fnr = it.ident,
                        periode = Periode(it.fom, it.tom),
                    ).toVedtakDetaljerResponse()
                    logger.debug { "OK /vedtak/detaljer - Systembruker ${systembruker.klientnavn}" }
                    call.respond(vedtak)
                },
            )
    }

    // Brukes av modia-personoversikt, tilleggsstønader og saas-proxy
    post("/vedtak/perioder") {
        logger.debug { "Mottatt POST kall på /vedtak/perioder - hent vedtak for fnr og periode" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /vedtak/perioder - hent vedtak for fnr og periode - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseVedtak()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /vedtak/perioder. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_VEDTAK}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_VEDTAK}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                {
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /vedtak/perioder. Underliggende feil: $it" }
                    call.respond(HttpStatusCode.BadRequest, it)
                },
                {
                    val vedtak = vedtakService.hentVedtaksperioder(
                        fnr = it.ident,
                        periode = Periode(it.fom, it.tom),
                    )

                    logger.debug { "OK /vedtak/perioder - Systembruker ${systembruker.klientnavn}" }
                    call.respond(vedtak)
                },
            )
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
