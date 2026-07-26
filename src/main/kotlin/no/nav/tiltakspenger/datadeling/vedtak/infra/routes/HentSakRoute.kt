package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.auth.medSystembruker
import no.nav.tiltakspenger.datadeling.infra.routes.VedtakReqDTO
import no.nav.tiltakspenger.datadeling.vedtak.HentSakService
import no.nav.tiltakspenger.datadeling.vedtak.HentetSak
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import java.time.LocalDateTime

/**
 * Konsumenter per juli 2026 (se doc/konsumenter.md): NKS/Salesforce via saas-proxy, som bruker dette som hovedendepunkt for å hente saksinformasjon.
 *
 * Response-DTO: [HentSakResponseDTO]
 */
// TODO jah: Avklar/test skillet mellom autentisert systembruker med feil rolle og token uten roller; rollesjekken i medSystembruker dekker i dag førstnevnte.
internal fun Route.hentSakRoute(
    hentSakService: HentSakService,
) {
    val logger = KotlinLogging.logger {}

    post("/vedtak/sak") {
        medSystembruker(Systembrukerrolle.LES_VEDTAK) { systembruker ->
            call.receive<VedtakReqDTO>().toFnr().fold(
                { error ->
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot /vedtak/sak. Underliggende feil: $error" }
                    call.respond(HttpStatusCode.BadRequest, error)
                },
                { fnr ->
                    hentSakService.hentSak(fnr = fnr).fold(
                        // Feilen er allerede logget i servicen via HttpKlientError.loggFeil.
                        ifLeft = {
                            call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil")
                        },
                        ifRight = { sak ->
                            if (sak == null) {
                                logger.debug { "Fant ingen sak for bruker - Systembruker ${systembruker.klientnavn}" }
                                call.respond404NotFound("Fant ingen sak for bruker", "sak_ikke_funnet")
                            } else {
                                call.respond(sak.toHentSakResponseDTO())
                            }
                        },
                    )
                },
            )
        }
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
     * Kan være null dersom det ikke finnes noen iverksatt søknadsbehandling enda, eller dersom kilden ikke har denne informasjonen (f.eks. Arena).
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
