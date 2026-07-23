package no.nav.tiltakspenger.datadeling.behandling.infra.routes

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.behandling.BehandlingService
import no.nav.tiltakspenger.datadeling.infra.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.MappingError
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakReqDTO
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.systembruker

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
) {
    val logger = KotlinLogging.logger {}
    // Ingen kjent konsument per juli 2026, muligens arena (se doc/konsumenter.md).
    // Returnerer åpne søknadsbehandlinger som overlapper med angitt periode
    post("/behandlinger/perioder") {
        logger.debug { "Mottatt POST kall på /behandlinger/perioder - hent åpne søknadsbehandlinger for periode og fnr" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /behandlinger/perioder - hent åpne søknadsbehandlinger for periode og fnr - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseBehandlinger()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /behandlinger/perioder. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_BEHANDLING}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_BEHANDLING}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        // TODO jah: Gi POST /behandlinger/perioder en egen BehandlingPerioderRequestDTO i denne route-filen i stedet for å gjenbruke VedtakReqDTO fra vedtak.
        // TODO jah: Avklar om manglende/blank fom/tom skal bety åpent intervall, eller om behandling/perioder skal kreve eksplisitt periode.
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                {
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /behandlinger/perioder. Underliggende feil: $it" }
                    call.respond(HttpStatusCode.BadRequest, it)
                },
                {
                    val behandlinger = behandlingService.hentBehandlingerForTp(
                        fnr = it.ident,
                        periode = Periode(it.fom, it.tom),
                    ).toResponse()
                    logger.debug { "OK /behandlinger/perioder - Systembruker ${systembruker.klientnavn}" }
                    call.respond(behandlinger)
                },
            )
    }

    // Konsumenter per juli 2026 (se doc/konsumenter.md): NKS/Salesforce via saas-proxy, muligens arena.
    post("/behandlinger/apne") {
        logger.debug { "Mottatt POST kall på /behandlinger/apne - hent åpne behandlinger for fnr" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        logger.debug { "Mottatt POST kall på /behandlinger/apne - hent åpne behandlinger for fnr - systembruker $systembruker" }

        if (!systembruker.roller.kanLeseBehandlinger()) {
            logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /behandlinger/apne. Underliggende feil: Mangler rollen ${Systembrukerrolle.LES_BEHANDLING}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LES_BEHANDLING}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }
        call.receive<BehandlingRequestDTO>().toFnr()
            .fold(
                {
                    logger.debug { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /behandlinger/apne. Underliggende feil: $it" }
                    call.respond(HttpStatusCode.BadRequest, it)
                },
                {
                    val behandlinger = behandlingService.hentApneBehandlinger(fnr = it).tilTpsakBehandlingResponseDTO()
                    logger.debug { "OK /behandlinger/apne - Systembruker ${systembruker.klientnavn}" }
                    call.respond(behandlinger)
                },
            )
    }
}

private data class BehandlingRequestDTO(
    val ident: String,
) {
    fun toFnr(): Either<MappingError, Fnr> {
        val fnr = try {
            Fnr.fromString(ident)
        } catch (_: Exception) {
            return MappingError(
                feilmelding = "Ident $ident er ugyldig. Må bestå av 11 siffer",
            ).left()
        }
        return fnr.right()
    }
}
