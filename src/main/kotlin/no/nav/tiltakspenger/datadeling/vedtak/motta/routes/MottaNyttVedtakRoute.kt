package no.nav.tiltakspenger.datadeling.vedtak.motta.routes

import arrow.core.Either
import arrow.core.getOrElse
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
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.texas.systembruker
import java.time.Clock
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
            val vedtak = Either.catch { body.toDomain(clock) }.getOrElse {
                log.error(it) { "Systembruker ${systembruker.klientnavn} fikk exception mot POST /vedtak" }
                this.call.respond(HttpStatusCode.BadRequest)
                return@withBody
            }

            mottaNyttVedtakService.motta(vedtak).fold(
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
    val vedtaksperiode: PeriodeDTO,
    val innvilgelsesperiode: PeriodeDTO? = null,
    val omgjørRammevedtakId: String? = null,
    val omgjortAvRammevedtakId: String? = null,
    val rettighet: RettighetDTO,
    val sakId: String,
    val opprettet: String,
    val barnetillegg: Barnetillegg?,
    val valgteHjemlerHarIkkeRettighet: List<ValgtHjemmelHarIkkeRettighetDTO>?,
) {
    fun toDomain(clock: Clock): TiltakspengerVedtak {
        return TiltakspengerVedtak(
            virkningsperiode = this.vedtaksperiode.toDomain(),
            innvilgelsesperiode = this.innvilgelsesperiode?.toDomain(),
            omgjørRammevedtakId = omgjørRammevedtakId,
            omgjortAvRammevedtakId = this.omgjortAvRammevedtakId,
            rettighet = rettighet.tilDomene(),
            vedtakId = this.vedtakId,
            sakId = this.sakId,
            opprettet = LocalDateTime.parse(this.opprettet),
            barnetillegg = this.barnetillegg,
            mottattTidspunkt = nå(clock),
            valgteHjemlerHarIkkeRettighet = valgteHjemlerHarIkkeRettighet?.map { it.tilDomene() },
        )
    }

    enum class RettighetDTO {
        TILTAKSPENGER,
        TILTAKSPENGER_OG_BARNETILLEGG,
        STANS,
        AVSLAG,
        OPPHØR,
        ;

        fun tilDomene() = when (this) {
            TILTAKSPENGER -> TiltakspengerVedtak.Rettighet.TILTAKSPENGER
            TILTAKSPENGER_OG_BARNETILLEGG -> TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
            STANS -> TiltakspengerVedtak.Rettighet.STANS
            AVSLAG -> TiltakspengerVedtak.Rettighet.AVSLAG
            OPPHØR -> TiltakspengerVedtak.Rettighet.OPPHØR
        }
    }

    enum class ValgtHjemmelHarIkkeRettighetDTO {
        DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK,
        ALDER,
        LIVSOPPHOLDSYTELSER,
        KVALIFISERINGSPROGRAMMET,
        INTRODUKSJONSPROGRAMMET,
        LONN_FRA_TILTAKSARRANGOR,
        LONN_FRA_ANDRE,
        INSTITUSJONSOPPHOLD,
        FREMMET_FOR_SENT,
        IKKE_LOVLIG_OPPHOLD,
        ;

        fun tilDomene() = when (this) {
            DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK
            ALDER -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.ALDER
            LIVSOPPHOLDSYTELSER -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.LIVSOPPHOLDSYTELSER
            KVALIFISERINGSPROGRAMMET -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET
            INTRODUKSJONSPROGRAMMET -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.INTRODUKSJONSPROGRAMMET
            LONN_FRA_TILTAKSARRANGOR -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.LONN_FRA_TILTAKSARRANGOR
            LONN_FRA_ANDRE -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.LONN_FRA_ANDRE
            INSTITUSJONSOPPHOLD -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD
            FREMMET_FOR_SENT -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.FREMMET_FOR_SENT
            IKKE_LOVLIG_OPPHOLD -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.IKKE_LOVLIG_OPPHOLD
        }
    }
}
