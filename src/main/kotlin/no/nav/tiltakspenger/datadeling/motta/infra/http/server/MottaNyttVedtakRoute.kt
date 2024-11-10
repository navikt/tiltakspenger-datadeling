package no.nav.tiltakspenger.datadeling.motta.infra.http.server

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.app.KanIkkeMottaVedtak
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.ErrorResponse
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond409Conflict
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tar i mot nye vedtak fra tiltakspenger-api og lagrer disse i datadeling.
 */
internal fun Route.mottaNyttVedtakRoute(
    mottaNyttVedtakService: MottaNyttVedtakService,
    tokenService: TokenService,
) {
    val log = KotlinLogging.logger {}
    post("/vedtak") {
        log.info("Mottatt nytt vedtak fra tiltakspenger-saksbehandling-api")
        this.call.withSystembruker(tokenService) { systembruker: Systembruker ->
            this.call.withBody<NyttVedktakJson> { body ->
                val vedtak = body.toDomain().getOrElse {
                    this.call.respond(HttpStatusCode.BadRequest, it.json)
                    return@withBody
                }
                mottaNyttVedtakService.motta(vedtak, systembruker).fold(
                    { error ->
                        when (error) {
                            is KanIkkeMottaVedtak.AlleredeLagret -> call.respond409Conflict(
                                "Vedtak med id ${vedtak.vedtakId} er allerede lagret",
                                "allerede_lagret",
                            )

                            is KanIkkeMottaVedtak.HarIkkeTilgang -> call.respond403Forbidden(
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

/**
 * @param antallDagerPerMeldeperiode antall dager en bruker skal melde seg for hver meldeperiode.
 * @param meldeperiodensLengde antall dager en meldeperiode varer, inkl. helg. Alltid 2 uker (14 dager) i MVP. Kan tenkes at den blir 1 uke i spesialtilfeller.
 */
private data class NyttVedktakJson(
    val vedtakId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDagerPerMeldeperiode: Int,
    val meldeperiodensLengde: Int,
    val dagsatsTiltakspenger: Int,
    val dagsatsBarnetillegg: Int,
    val antallBarn: Int,
    val relaterteTiltak: String,
    val rettighet: String,
    val sakId: String,
    val saksnummer: String,
    val fnr: String,
    val mottattTidspunkt: String,
    val opprettetTidspunkt: String,
) {
    fun toDomain(): Either<ErrorResponse, TiltakspengerVedtak> {
        return TiltakspengerVedtak(
            periode = Periode(this.fom, this.tom),
            antallDagerPerMeldeperiode = this.antallDagerPerMeldeperiode,
            meldeperiodensLengde = this.meldeperiodensLengde,
            dagsatsTiltakspenger = this.dagsatsTiltakspenger,
            dagsatsBarnetillegg = this.dagsatsBarnetillegg,
            antallBarn = this.antallBarn,
            tiltaksgjennomføringId = this.relaterteTiltak,
            rettighet = when (this.rettighet) {
                "TILTAKSPENGER" -> TiltakspengerVedtak.Rettighet.TILTAKSPENGER
                else -> return ErrorResponse(
                    json = ErrorJson(
                        melding = "Ukjent rettighet: '${this.rettighet}'. Lovlige verdier: 'TILTAKSPENGER'",
                        kode = "ukjent_rettighet",
                    ),
                    httpStatus = HttpStatusCode.BadRequest,
                ).left()
            },
            vedtakId = this.vedtakId,
            sakId = this.sakId,
            saksnummer = this.saksnummer,
            kilde = "tp",
            fnr = Fnr.fromString(this.fnr),
            mottattTidspunkt = LocalDateTime.parse(this.mottattTidspunkt),
            opprettetTidspunkt = LocalDateTime.parse(this.opprettetTidspunkt),
        ).right()
    }
}
