package no.nav.tiltakspenger.datadeling.meldekort.motta.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.meldekort.db.GodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.meldekort.domene.GodkjentMeldekort
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.LocalDate
import java.time.LocalDateTime

fun Route.mottaGodkjentMeldekortRoute(
    godkjentMeldekortRepo: GodkjentMeldekortRepo,
) {
    val log = KotlinLogging.logger {}
    post("/meldekort") {
        log.debug { "Mottatt POST kall på /meldekort - lagre godkjent meldekort fra tiltakspenger-saksbehandling-api" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        if (!systembruker.roller.kanLagreTiltakspengerHendelser()) {
            log.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /meldekort. Underliggende feil: Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }

        call.withBody<GodkjentMeldekortDTO> { body ->
            val godkjentMeldekort = body.toDomain()
            try {
                godkjentMeldekortRepo.lagre(godkjentMeldekort)
                call.respond(HttpStatusCode.OK)
                log.debug { "Systembruker ${systembruker.klientnavn} lagret meldekort OK." }
            } catch (e: Exception) {
                log.error { "Systembruker ${systembruker.klientnavn} fikk 500 Internal Server Error mot POST /meldekort. Underliggende feil: ${e.message}" }
                call.respond500InternalServerError(
                    "Meldekort kunne ikke lagres siden en ukjent feil oppstod",
                    "ukjent_feil",
                )
                return@withBody
            }
        }
    }
}

private data class GodkjentMeldekortDTO(
    val kjedeId: String,
    val sakId: String,
    val meldeperiodeId: String,
    val mottattTidspunkt: LocalDateTime?,
    val vedtattTidspunkt: LocalDateTime,
    val behandletAutomatisk: Boolean,
    val korrigert: Boolean,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortdager: List<MeldekortDagDTO>,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
) {
    data class MeldekortDagDTO(
        val dato: LocalDate,
        val status: String,
        val reduksjon: String,
    ) {
        fun toDomain(): GodkjentMeldekort.MeldekortDag {
            return GodkjentMeldekort.MeldekortDag(
                dato = dato,
                status = when (status) {
                    "DELTATT_UTEN_LONN_I_TILTAKET" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET
                    "DELTATT_MED_LONN_I_TILTAKET" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.DELTATT_MED_LONN_I_TILTAKET
                    "FRAVAER_SYK" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.FRAVAER_SYK
                    "FRAVAER_SYKT_BARN" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.FRAVAER_SYKT_BARN
                    "FRAVAER_GODKJENT_AV_NAV" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.FRAVAER_GODKJENT_AV_NAV
                    "FRAVAER_ANNET" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.FRAVAER_ANNET
                    "IKKE_BESVART" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.IKKE_BESVART
                    "IKKE_TILTAKSDAG" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.IKKE_TILTAKSDAG
                    "IKKE_RETT_TIL_TILTAKSPENGER" -> GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                    else -> throw IllegalArgumentException("Mottatt ukjent status for meldekortdag: ${this.status}")
                },
                reduksjon = when (reduksjon) {
                    "INGEN_REDUKSJON" -> GodkjentMeldekort.MeldekortDag.Reduksjon.INGEN_REDUKSJON
                    "UKJENT" -> GodkjentMeldekort.MeldekortDag.Reduksjon.UKJENT
                    "YTELSEN_FALLER_BORT" -> GodkjentMeldekort.MeldekortDag.Reduksjon.YTELSEN_FALLER_BORT
                    else -> throw IllegalArgumentException("Mottatt ukjent reduksjon for meldekortdag: ${this.reduksjon}")
                },
            )
        }
    }

    fun toDomain(): GodkjentMeldekort {
        return GodkjentMeldekort(
            kjedeId = kjedeId,
            sakId = SakId.fromString(sakId),
            meldeperiodeId = MeldeperiodeId.fromString(meldeperiodeId),
            mottattTidspunkt = mottattTidspunkt,
            vedtattTidspunkt = vedtattTidspunkt,
            behandletAutomatisk = behandletAutomatisk,
            korrigert = korrigert,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            meldekortdager = meldekortdager.map { it.toDomain() },
            opprettet = opprettet,
            sistEndret = sistEndret,
        )
    }
}
