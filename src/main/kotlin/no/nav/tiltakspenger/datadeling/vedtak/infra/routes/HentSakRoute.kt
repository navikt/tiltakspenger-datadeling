package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.vedtak.HentSakService
import no.nav.tiltakspenger.datadeling.vedtak.HentetSak
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.LocalDateTime

// Brukes av saas-proxy (NKS/Salesforce) som hovedendepunkt for å hente saksinformasjon.
internal fun Route.hentSakRoute(
    hentSakService: HentSakService,
) {
    val logger = KotlinLogging.logger {}

    post("/vedtak/sak") {
        logger.debug { "Mottatt POST kall på /vedtak/sak - hent sak for fnr" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /vedtak/sak - hent sak for fnr - systembruker $systembruker" }

        // TODO jah: Avklar/test skillet mellom autentisert systembruker med feil rolle og token uten roller; route-grenen dekker i dag førstnevnte.
        if (!systembruker.roller.kanLeseVedtak()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot /vedtak/sak. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_VEDTAK}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_VEDTAK}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<VedtakReqDTO>().toSakRequest()
            .fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /vedtak/sak. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { fnr ->
                    val sak = hentSakService.hentSak(fnr = fnr)
                    if (sak == null) {
                        logger.debug { "Fant ingen sak for bruker - Systembruker ${systembruker.klientnavn}" }
                        call.respond404NotFound("Fant ingen sak for bruker", "sak_ikke_funnet")
                        return@post
                    }
                    logger.debug { "OK /vedtak/sak - Systembruker ${systembruker.klientnavn}" }
                    call.respond(sak.toHentSakResponseDTO())
                },
            )
    }
}

/**
 * Response-DTO brukt kun av `POST /vedtak/sak` (saas-proxy).
 *
 * Skiller seg fra sak-feltene i de deprecated responsene ved at den inneholder
 * [iverksattSoknadsbehandlingTidspunkt].
 */
private data class HentSakResponseDTO(
    val sakId: String,
    val saksnummer: String,
    val kilde: String,
    val status: String,
    val opprettetDato: LocalDateTime,
    /**
     * Tidspunktet første søknadsbehandling ble iverksatt for denne saken.
     * Kan være null dersom det ikke finnes noen iverksatt søknadsbehandling enda,
     * eller dersom kilden ikke har denne informasjonen (f.eks. Arena).
     */
    val iverksattSoknadsbehandlingTidspunkt: LocalDateTime?,
)

private fun HentetSak.toHentSakResponseDTO() = HentSakResponseDTO(
    sakId = sakId,
    saksnummer = saksnummer,
    kilde = kilde,
    status = status,
    opprettetDato = opprettetDato,
    iverksattSoknadsbehandlingTidspunkt = iverksattSoknadsbehandlingTidspunkt,
)
