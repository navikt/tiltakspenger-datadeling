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
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.service.KanIkkeHenteVedtak
import no.nav.tiltakspenger.datadeling.service.VedtakstidslinjeService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withBruker
import no.nav.tiltakspenger.libs.common.Bruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periodisering.Periode

fun Route.vedtakTidslinjeRoute(
    vedtakstidslinjeService: VedtakstidslinjeService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}

    post("/vedtak/tidslinje") {
        logger.debug { "Mottatt POST kall på /vedtak/tidslinje - hent vedtakstidslinje for fnr og periode" }
        call.withBruker<Bruker<*, *>>(
            tokenService = tokenService,
            svarMed403HvisIngenSaksbehandlerRoller = false,
            svarMed403HvisIngenScopes = true,
            svarMed403HvisIngenSystembrukerRoller = true,
        ) { bruker: Bruker<*, *> ->
            logger.debug { "Mottatt POST kall på /vedtak/tidslinje - hent vedtakstidslinje for fnr og periode. Klientnavn: ${bruker.klientnavn}, klientId: ${bruker.klientId}, navIdent: ${bruker.navIdent}" }
            call.receive<VedtakReqDTO>().toVedtakRequest()
                .fold(
                    { error ->
                        logger.error { "Bruker fikk 400 Bad Request mot /vedtak/tidslinje. Underliggende feil: $error. Klientnavn: ${bruker.klientnavn}, klientId: ${bruker.klientId}, navIdent: ${bruker.navIdent}" }
                        call.respond(HttpStatusCode.BadRequest, error)
                    },
                    { request ->
                        val jsonPayload = vedtakstidslinjeService.hentTidslinje(
                            fnr = Fnr.fromString(request.ident),
                            periode = Periode(request.fom, request.tom),
                            kilder = request.kilder.map {
                                when (it) {
                                    "tp" -> Kilde.TILTAKSPENGER
                                    "arena" -> Kilde.ARENA
                                    else -> {
                                        logger.warn { "Bruker fikk 400 Bad Request mot /vedtak/tidslinje. Ukjent kilde $it, må være en av 'tp' eller 'arena'. Klientnavn: ${bruker.klientnavn}, klientId: ${bruker.klientId}, navIdent: ${bruker.navIdent}" }
                                        call.respond400BadRequest(
                                            "Ukjent kilde $it, må være en av 'tp' eller 'arena'",
                                            "ukjent_kilde",
                                        )
                                        return@withBruker
                                    }
                                }
                            }.toSet(),
                            bruker = bruker,
                        ).getOrElse { error ->
                            when (error) {
                                is KanIkkeHenteVedtak.HarIkkeTilgang -> {
                                    logger.error { "Systembruker fikk 403 Forbidden mot /vedtak/tidslinje. Underliggende feil: $error. Klientnavn: ${bruker.klientnavn}, klientId: ${bruker.klientId}, navIdent: ${bruker.navIdent}" }
                                    call.respond403Forbidden(
                                        "Mangler rollen ${error.kreverEnAvRollene}. Har rollene: ${error.harRollene}",
                                        "mangler_rolle",
                                    )
                                }
                            }
                            return@withBruker
                        }.toJson()
                        logger.debug { "OK /vedtak/tidslinje - Klientnavn: ${bruker.klientnavn}, klientId: ${bruker.klientId}, navIdent: ${bruker.navIdent}" }
                        call.respondText(
                            status = HttpStatusCode.OK,
                            text = jsonPayload,
                            contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        )
                    },
                )
        }
    }
}
