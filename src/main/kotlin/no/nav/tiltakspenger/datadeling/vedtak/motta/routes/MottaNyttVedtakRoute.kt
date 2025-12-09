package no.nav.tiltakspenger.datadeling.vedtak.motta.routes

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.getSystemBrukerMapper
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.vedtak.motta.KanIkkeMottaVedtak
import no.nav.tiltakspenger.datadeling.vedtak.motta.MottaNyttVedtakService
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.ErrorResponse
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tar i mot nye vedtak fra tiltakspenger-api og lagrer disse i datadeling.
 */
internal fun Route.mottaNyttVedtakRoute(
    mottaNyttVedtakService: MottaNyttVedtakService,
    clock: Clock,
) {
    val log = KotlinLogging.logger {}
    post("/vedtak") {
        log.debug { "Mottatt POST kall på /vedtak - lagre vedtak fra tiltakspenger-saksbehandling-api" }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        if (!systembruker.roller.kanLagreTiltakspengerHendelser()) {
            log.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot POST /vedtak. Underliggende feil: Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}" }
            call.respond403Forbidden(
                "Mangler rollen ${Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER}. Har rollene: ${systembruker.roller.toList()}",
                "mangler_rolle",
            )
            return@post
        }

        this.call.withBody<NyttVedktakJson> { body ->
            val vedtak = body.toDomain(clock).getOrElse {
                log.error { "Systembruker ${systembruker.klientnavn} fikk 400 Bad Request mot POST /vedtak. Underliggende feil: $it" }
                this.call.respond(HttpStatusCode.BadRequest, it.json)
                return@withBody
            }
            val fnr = Fnr.fromString(body.fnr)
            val saksnummer = body.saksnummer
            mottaNyttVedtakService.motta(vedtak, fnr, saksnummer).fold(
                { error ->
                    when (error) {
                        is KanIkkeMottaVedtak.Persisteringsfeil -> {
                            log.error { "Systembruker ${systembruker.klientnavn} fikk 500 Internal Server Error mot POST /vedtak. Underliggende feil: $error" }
                            call.respond500InternalServerError(
                                "Vedtak med id ${vedtak.vedtakId} kunne ikke lagres siden en ukjent feil oppstod",
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

private data class NyttVedktakJson(
    val vedtakId: String,
    @Deprecated("Bytt til å bruke en kombinasjon av virkningsperiode og innvilgelsesperiode, slett fom og tom etter dette.")
    val fom: LocalDate,
    @Deprecated("Bytt til å bruke en kombinasjon av virkningsperiode og innvilgelsesperiode, slett fom og tom etter dette.")
    val tom: LocalDate,
    // 2025-10-21: Lagt til virkningsperiode, innvilgelsesperiode, omgjørRammevedtakId og omgjortAvRammevedtakId og defaulter alle til null.
    // TODO jah: Sett virkningsperiode som non-nullable etter vi har deployet tilsvarende endringer i tiltakspenger-saksbehandling-api.
    val virkningsperiode: PeriodeDTO? = null,
    val innvilgelsesperiode: PeriodeDTO? = null,
    val omgjørRammevedtakId: String? = null,
    val omgjortAvRammevedtakId: String? = null,
    val rettighet: String,
    val sakId: String,
    val saksnummer: String,
    val fnr: String,
    val opprettet: String,
    val barnetillegg: Barnetillegg?,
    val valgteHjemlerHarIkkeRettighet: List<String>?,
) {
    fun toDomain(clock: Clock): Either<ErrorResponse, TiltakspengerVedtak> {
        val rettighet = when (this.rettighet) {
            "TILTAKSPENGER" -> TiltakspengerVedtak.Rettighet.TILTAKSPENGER
            "TILTAKSPENGER_OG_BARNETILLEGG" -> TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
            "STANS" -> TiltakspengerVedtak.Rettighet.STANS
            "AVSLAG" -> TiltakspengerVedtak.Rettighet.AVSLAG
            else -> return ErrorResponse(
                json = ErrorJson(
                    melding = "Ukjent rettighet: '${this.rettighet}'.",
                    kode = "ukjent_rettighet",
                ),
                httpStatus = HttpStatusCode.BadRequest,
            ).left()
        }
        // TODO jah: Fjern deprecatedPeriode etter at tiltakspenger-saksbehandling-api har tatt i bruk de nye feltene.
        val deprecatedPeriode = Periode(this.fom, this.tom)
        return TiltakspengerVedtak(
            virkningsperiode = this.virkningsperiode?.toDomain() ?: deprecatedPeriode,
            innvilgelsesperiode = this.innvilgelsesperiode?.toDomain() ?: when (rettighet) {
                TiltakspengerVedtak.Rettighet.TILTAKSPENGER, TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> deprecatedPeriode
                TiltakspengerVedtak.Rettighet.STANS, TiltakspengerVedtak.Rettighet.AVSLAG -> null
            },
            omgjørRammevedtakId = omgjørRammevedtakId,
            omgjortAvRammevedtakId = this.omgjortAvRammevedtakId,
            rettighet = rettighet,
            vedtakId = this.vedtakId,
            sakId = this.sakId,
            opprettet = LocalDateTime.parse(this.opprettet),
            barnetillegg = this.barnetillegg,
            mottattTidspunkt = nå(clock),
            valgteHjemlerHarIkkeRettighet = valgteHjemlerHarIkkeRettighet?.map { toValgtHjemmelHarIkkeRettighet(it) },
        ).right()
    }

    private fun toValgtHjemmelHarIkkeRettighet(valgtHjemmel: String) = when (valgtHjemmel) {
        "DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK
        "ALDER" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.ALDER
        "LIVSOPPHOLDSYTELSER" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.LIVSOPPHOLDSYTELSER
        "KVALIFISERINGSPROGRAMMET" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET
        "INTRODUKSJONSPROGRAMMET" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.INTRODUKSJONSPROGRAMMET
        "LONN_FRA_TILTAKSARRANGOR" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.LONN_FRA_TILTAKSARRANGOR
        "LONN_FRA_ANDRE" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.LONN_FRA_ANDRE
        "INSTITUSJONSOPPHOLD" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD
        "FREMMET_FOR_SENT" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.FREMMET_FOR_SENT
        else -> throw IllegalArgumentException("Ukjent valgt hjemmel for stans/avslag: $valgtHjemmel")
    }
}
