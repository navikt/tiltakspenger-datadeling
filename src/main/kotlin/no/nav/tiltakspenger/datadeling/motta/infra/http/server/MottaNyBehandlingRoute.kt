package no.nav.tiltakspenger.datadeling.motta.infra.http.server

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.motta.app.KanIkkeMottaBehandling
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyBehandlingService
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.datadeling.DatadelingBehandlingDTO
import no.nav.tiltakspenger.libs.ktor.common.ErrorResponse
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.Clock

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

fun DatadelingBehandlingDTO.toDomain(clock: Clock): Either<ErrorResponse, TiltakspengerBehandling> {
    return TiltakspengerBehandling(
        behandlingId = this.behandlingId,
        sakId = this.sakId,
        periode = Periode(fraOgMed = this.fraOgMed, tilOgMed = this.tilOgMed),
        behandlingStatus = when (this.behandlingStatus) {
            DatadelingBehandlingDTO.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> TiltakspengerBehandling.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
            DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BEHANDLING -> TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING
            DatadelingBehandlingDTO.Behandlingsstatus.VEDTATT -> TiltakspengerBehandling.Behandlingsstatus.VEDTATT
            DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BESLUTNING -> TiltakspengerBehandling.Behandlingsstatus.UNDER_BESLUTNING
            DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BESLUTNING -> TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BESLUTNING
            DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BEHANDLING -> TiltakspengerBehandling.Behandlingsstatus.UNDER_BEHANDLING
            DatadelingBehandlingDTO.Behandlingsstatus.AVBRUTT -> TiltakspengerBehandling.Behandlingsstatus.AVBRUTT
        },
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        iverksattTidspunkt = this.iverksattTidspunkt,
        fnr = Fnr.fromString(this.fnr),
        saksnummer = this.saksnummer,
        søknadJournalpostId = this.søknadJournalpostId,
        opprettetTidspunktSaksbehandlingApi = this.opprettetTidspunktSaksbehandlingApi,
        mottattTidspunktDatadeling = nå(clock),
    ).right()
}
