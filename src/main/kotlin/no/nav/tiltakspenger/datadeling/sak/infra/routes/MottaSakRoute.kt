package no.nav.tiltakspenger.datadeling.sak.infra.routes

import arrow.core.Either
import arrow.core.flatMap
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.sak.KanIkkeMottaSak
import no.nav.tiltakspenger.datadeling.sak.MottaSakKommando
import no.nav.tiltakspenger.datadeling.sak.MottaSakService
import no.nav.tiltakspenger.datadeling.sak.NySak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.LocalDateTime

internal fun Route.mottaSakRoute(
    mottaSakService: MottaSakService,
) {
    val log = KotlinLogging.logger {}
    post("/sak") {
        log.debug { "Mottatt POST kall på /sak - lagre sak fra tiltakspenger-saksbehandling-api" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        if (!systembruker.roller.kanLagreTiltakspengerHendelser()) {
            log.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /sak. Underliggende feil: Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }

        call.withBody<MottaSakRequestDTO> { body ->
            Either.catch { body.toCommand() }
                .mapLeft { KanIkkeMottaSak.UkjentFeil }
                .flatMap { mottaSakService.motta(it) }
                .fold(
                    {
                        log.error { "Systembruker ${systembruker.klientnavn} fikk 500 Internal Server Error mot POST /sak." }
                        call.respond500InternalServerError(
                            "Sak kunne ikke lagres siden en ukjent feil oppstod",
                            "ukjent_feil",
                        )
                        return@withBody
                    },
                    {
                        call.respond(HttpStatusCode.OK)
                        log.debug { "Systembruker ${systembruker.klientnavn} lagret sak OK." }
                    },
                )
        }
    }
}

private data class MottaSakRequestDTO(
    val id: String,
    val fnr: String,
    val saksnummer: String,
    val opprettet: LocalDateTime,
) {
    fun toCommand(): MottaSakKommando = MottaSakKommando(
        nySak = NySak(
            id = SakId.fromString(id),
            fnr = Fnr.fromString(fnr),
            saksnummer = Saksnummer(saksnummer),
            opprettet = opprettet,
        ),
    )
}
