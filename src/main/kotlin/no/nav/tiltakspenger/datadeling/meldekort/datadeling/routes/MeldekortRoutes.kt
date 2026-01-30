package no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.MeldekortService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakReqDTO
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.systembruker

fun Route.meldekortRoutes(meldekortService: MeldekortService) {
    val logger = KotlinLogging.logger {}

    // Brukes av saas-proxy
    post("/meldekort/detaljer") {
        logger.debug { "Mottatt POST kall på /meldekort/detaljer - hent meldeperioder og meldekort for fnr og periode" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /meldekort/detaljer - hent meldeperioder og meldekort for fnr og periode - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseMeldekort()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /meldekort/detaljer. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_MELDEKORT}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_MELDEKORT}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /meldekort/detaljer. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                {
                    val response = meldekortService.hentMeldekort(
                        fnr = it.ident,
                        periode = Periode(it.fom, it.tom),
                    )
                    logger.debug { "OK /meldekort/detaljer - Systembruker ${systembruker.klientnavn}" }
                    call.respond(response)
                },
            )
    }
}
