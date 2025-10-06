package no.nav.tiltakspenger.datadeling.meldekort.motta.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.meldekort.db.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.LocalDate
import java.time.LocalDateTime

fun Route.mottaMeldeperioderRoute(
    meldeperiodeRepo: MeldeperiodeRepo,
) {
    val log = KotlinLogging.logger {}
    post("/meldeperioder") {
        log.debug { "Mottatt POST kall på /meldeperioder - lagre meldeperioder fra tiltakspenger-saksbehandling-api" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        if (!systembruker.roller.kanLagreTiltakspengerHendelser()) {
            log.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /meldeperioder. Underliggende feil: Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }

        call.withBody<MeldeperioderDTO> { body ->
            val meldeperioder = body.toDomain()
            if (meldeperioder.isEmpty()) {
                log.info { "Ingen meldeperioder for sakId ${body.sakId} gir rett, lagrer ingenting" }
                call.respond(HttpStatusCode.OK)
                return@withBody
            }
            try {
                meldeperiodeRepo.lagre(meldeperioder)
                call.respond(HttpStatusCode.OK)
                log.debug { "Systembruker ${systembruker.klientnavn} lagret meldeperioder OK." }
            } catch (e: Exception) {
                log.error { "Systembruker ${systembruker.klientnavn} fikk 500 Internal Server Error mot POST /meldeperioder. Underliggende feil: ${e.message}" }
                call.respond500InternalServerError(
                    "Meldeperioder kunne ikke lagres siden en ukjent feil oppstod",
                    "ukjent_feil",
                )
                return@withBody
            }
        }
    }
}

private data class MeldeperioderDTO(
    val fnr: String,
    val sakId: String,
    val saksnummer: String,
    val meldeperioder: List<MeldeperiodeDTO>,
) {
    data class MeldeperiodeDTO(
        val id: String,
        val kjedeId: String,
        val opprettet: LocalDateTime,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val antallDagerForPeriode: Int,
        val girRett: Map<LocalDate, Boolean>,
    )

    fun toDomain(): List<Meldeperiode> {
        return meldeperioder.map {
            Meldeperiode(
                id = MeldeperiodeId.fromString(it.id),
                kjedeId = it.kjedeId,
                fnr = Fnr.fromString(fnr),
                sakId = SakId.fromString(sakId),
                saksnummer = saksnummer,
                opprettet = it.opprettet,
                fraOgMed = it.fraOgMed,
                tilOgMed = it.tilOgMed,
                maksAntallDagerForPeriode = it.antallDagerForPeriode,
                girRett = it.girRett,
            )
        }
    }
}
