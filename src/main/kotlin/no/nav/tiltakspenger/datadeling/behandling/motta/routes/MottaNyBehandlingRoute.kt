package no.nav.tiltakspenger.datadeling.behandling.motta.routes

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.behandling.motta.KanIkkeMottaBehandling
import no.nav.tiltakspenger.datadeling.behandling.motta.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.ktor.common.ErrorResponse
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tar i mot behandlinger fra tiltakspenger-api og lagrer disse i datadeling.
 * Dersom vi har mottatt behandlingen før, overlagrer vi den usett.
 */
internal fun Route.mottaNyBehandlingRoute(
    mottaNyBehandlingService: MottaNyBehandlingService,
    clock: Clock,
) {
    val log = KotlinLogging.logger {}
    post("/behandling") {
        log.debug { "Mottatt POST kall på /behandling - lagre behandling fra tiltakspenger-saksbehandling-api" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        if (!systembruker.roller.kanLagreTiltakspengerHendelser()) {
            log.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /behandling. Underliggende feil: Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }

        this.call.withBody<DatadelingBehandlingDTO> { body ->
            val behandling = body.toDomain(clock).getOrElse {
                log.error { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /behandling. Underliggende feil: $it" }
                this.call.respond(HttpStatusCode.BadRequest, it.json)
                return@withBody
            }
            mottaNyBehandlingService.motta(behandling).fold(
                { error ->
                    when (error) {
                        is KanIkkeMottaBehandling.Persisteringsfeil -> {
                            log.error { "Systembruker ${systembruker.klientnavn} fikk 500 Internal Server Error mot POST /behandling. Underliggende feil: $error" }
                            call.respond500InternalServerError(
                                "Behandling med id ${behandling.behandlingId} kunne ikke lagres siden en ukjent feil oppstod",
                                "ukjent_feil",
                            )
                        }
                    }
                },
                {
                    this.call.respond(HttpStatusCode.OK)
                    log.debug { "Systembruker ${systembruker.klientnavn} lagret behandling OK." }
                },
            )
        }
    }
}

data class DatadelingBehandlingDTO(
    val behandlingId: String,
    val sakId: String,
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
    val behandlingStatus: Behandlingsstatus,
    val saksbehandler: String?,
    val beslutter: String?,
    val iverksattTidspunkt: LocalDateTime?,
    val opprettetTidspunktSaksbehandlingApi: LocalDateTime,
    val behandlingstype: Behandlingstype,
    val sistEndret: LocalDateTime,
) {
    enum class Behandlingsstatus {
        UNDER_AUTOMATISK_BEHANDLING,
        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        UNDER_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        IKKE_RETT_TIL_TILTAKSPENGER,
    }

    enum class Behandlingstype {
        SOKNADSBEHANDLING,
        REVURDERING,
        MELDEKORTBEHANDLING,
    }

    fun toDomain(clock: Clock): Either<ErrorResponse, TiltakspengerBehandling> {
        return TiltakspengerBehandling(
            behandlingId = this.behandlingId,
            sakId = this.sakId,
            periode = if (this.fraOgMed != null && this.tilOgMed != null) {
                Periode(fraOgMed = this.fraOgMed, tilOgMed = this.tilOgMed)
            } else {
                null
            },
            behandlingStatus = when (this.behandlingStatus) {
                Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> TiltakspengerBehandling.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                Behandlingsstatus.KLAR_TIL_BEHANDLING -> TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING
                Behandlingsstatus.VEDTATT -> TiltakspengerBehandling.Behandlingsstatus.VEDTATT
                Behandlingsstatus.UNDER_BESLUTNING -> TiltakspengerBehandling.Behandlingsstatus.UNDER_BESLUTNING
                Behandlingsstatus.KLAR_TIL_BESLUTNING -> TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BESLUTNING
                Behandlingsstatus.UNDER_BEHANDLING -> TiltakspengerBehandling.Behandlingsstatus.UNDER_BEHANDLING
                Behandlingsstatus.AVBRUTT -> TiltakspengerBehandling.Behandlingsstatus.AVBRUTT
                Behandlingsstatus.GODKJENT -> TiltakspengerBehandling.Behandlingsstatus.GODKJENT
                Behandlingsstatus.AUTOMATISK_BEHANDLET -> TiltakspengerBehandling.Behandlingsstatus.AUTOMATISK_BEHANDLET
                Behandlingsstatus.IKKE_RETT_TIL_TILTAKSPENGER -> TiltakspengerBehandling.Behandlingsstatus.IKKE_RETT_TIL_TILTAKSPENGER
            },
            saksbehandler = this.saksbehandler,
            beslutter = this.beslutter,
            iverksattTidspunkt = this.iverksattTidspunkt,
            opprettetTidspunktSaksbehandlingApi = this.opprettetTidspunktSaksbehandlingApi,
            mottattTidspunktDatadeling = nå(clock),
            behandlingstype = when (this.behandlingstype) {
                Behandlingstype.SOKNADSBEHANDLING -> TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING
                Behandlingstype.REVURDERING -> TiltakspengerBehandling.Behandlingstype.REVURDERING
                Behandlingstype.MELDEKORTBEHANDLING -> TiltakspengerBehandling.Behandlingstype.MELDEKORTBEHANDLING
            },
            sistEndret = this.sistEndret,
        ).right()
    }
}
