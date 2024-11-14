package no.nav.tiltakspenger.datadeling.motta.infra.http.server

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.motta.app.KanIkkeMottaBehandling
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyBehandlingService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.datadeling.DatadelingBehandlingDTO
import no.nav.tiltakspenger.libs.ktor.common.ErrorResponse
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.periodisering.Periode

/**
 * Tar i mot behandlinger fra tiltakspenger-api og lagrer disse i datadeling.
 * Dersom vi har mottatt behandlingen før, overlagrer vi den usett.
 */
internal fun Route.mottaNyBehandlingRoute(
    mottaNyBehandlingService: MottaNyBehandlingService,
    tokenService: TokenService,
) {
    val log = KotlinLogging.logger {}
    post("/behandling") {
        log.info("Mottatt behandling fra tiltakspenger-saksbehandling-api")
        this.call.withSystembruker(tokenService) { systembruker: Systembruker ->
            this.call.withBody<DatadelingBehandlingDTO> { body ->
                val behandling = body.toDomain().getOrElse {
                    this.call.respond(HttpStatusCode.BadRequest, it.json)
                    return@withBody
                }
                mottaNyBehandlingService.motta(behandling, systembruker).fold(
                    { error ->
                        when (error) {
                            is KanIkkeMottaBehandling.Persisteringsfeil -> call.respond500InternalServerError(
                                "Behandling med id ${behandling.behandlingId} kunne ikke lagres siden en ukjent feil oppstod",
                                "ukjent_feil",
                            )

                            is KanIkkeMottaBehandling.HarIkkeTilgang -> call.respond403Forbidden(
                                "Mangler rollen ${error.kreverEnAvRollene}. Har rollene: ${error.harRollene}",
                                "mangler_rolle",
                            )
                        }
                    },
                    { this.call.respond(HttpStatusCode.OK) },
                )
            }
        }
    }
}

fun DatadelingBehandlingDTO.toDomain(): Either<ErrorResponse, TiltakspengerBehandling> {
    return TiltakspengerBehandling(
        behandlingId = this.behandlingId,
        sakId = this.sakId,
        periode = Periode(fraOgMed = this.fraOgMed, tilOgMed = this.tilOgMed),
        behandlingStatus = when (this.behandlingStatus) {
            DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BEHANDLING -> TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING
            DatadelingBehandlingDTO.Behandlingsstatus.INNVILGET -> TiltakspengerBehandling.Behandlingsstatus.INNVILGET
            DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BESLUTNING -> TiltakspengerBehandling.Behandlingsstatus.UNDER_BESLUTNING
            DatadelingBehandlingDTO.Behandlingsstatus.KLAR_TIL_BESLUTNING -> TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BESLUTNING
            DatadelingBehandlingDTO.Behandlingsstatus.UNDER_BEHANDLING -> TiltakspengerBehandling.Behandlingsstatus.UNDER_BEHANDLING
        },
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        iverksattTidspunkt = this.iverksattTidspunkt,
        fnr = Fnr.fromString(this.fnr),
        saksnummer = this.saksnummer,
        søknadJournalpostId = this.søknadJournalpostId,
        opprettetTidspunktSaksbehandlingApi = this.opprettetTidspunktSaksbehandlingApi,
        tiltaksdeltagelse = TiltakspengerBehandling.Tiltaksdeltagelse(
            tiltaksnavn = this.tiltak.tiltakNavn,
            eksternTiltaksdeltakerId = this.tiltak.eksternTiltakdeltakerId,
            eksternGjennomføringId = this.tiltak.gjennomføringId,
        ),
    ).right()
}
