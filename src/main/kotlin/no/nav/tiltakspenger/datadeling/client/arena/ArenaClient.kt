package no.nav.tiltakspenger.datadeling.client.arena

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.felles.app.exception.egendefinerteFeil.KallTilVedtakFeilException
import no.nav.tiltakspenger.datadeling.felles.infra.http.klient.httpClientCIO
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

val log = KotlinLogging.logger {}

class ArenaClient(
    private val baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    private val httpClient: HttpClient = httpClientCIO(),
) {
    companion object {
        const val NAV_CALL_ID_HEADER = "tiltakspenger-datadeling"
    }

    private data class ArenaPeriodeResponseDTO(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate?,
    )

    private data class ArenaResponseDTO(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate?,
        val antallDager: Double,
        val dagsatsTiltakspenger: Int,
        val dagsatsBarnetillegg: Int,
        val antallBarn: Int,
        val relaterteTiltak: String,
        val rettighet: RettighetDTO,
        val vedtakId: Long,
        val sakId: Long,
    )

    private enum class RettighetDTO {
        TILTAKSPENGER,
        BARNETILLEGG,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }

    data class ArenaRequestDTO(
        val ident: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    suspend fun hentVedtak(fnr: Fnr, periode: Periode): List<Vedtak> {
        val dto = hentVedtak(
            ArenaRequestDTO(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        ) ?: return emptyList()

        return dto.map {
            Vedtak(
                periode = Periode(it.fraOgMed, it.tilOgMed ?: LocalDate.of(9999, 12, 31)),
                rettighet = when (it.rettighet) {
                    RettighetDTO.TILTAKSPENGER -> no.nav.tiltakspenger.datadeling.domene.Rettighet.TILTAKSPENGER
                    RettighetDTO.BARNETILLEGG -> no.nav.tiltakspenger.datadeling.domene.Rettighet.BARNETILLEGG
                    RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG -> no.nav.tiltakspenger.datadeling.domene.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
                    RettighetDTO.INGENTING -> no.nav.tiltakspenger.datadeling.domene.Rettighet.INGENTING
                },
                vedtakId = it.vedtakId.toString(),
                sakId = it.sakId.toString(),
                saksnummer = null,
                kilde = Kilde.ARENA,
                fnr = fnr,
                antallBarn = it.antallBarn,
                dagsatsTiltakspenger = if (it.rettighet == RettighetDTO.TILTAKSPENGER || it.rettighet == RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG) {
                    it.dagsatsTiltakspenger
                } else {
                    null
                },
                dagsatsBarnetillegg = if (it.rettighet == RettighetDTO.BARNETILLEGG || it.rettighet == RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG) {
                    it.dagsatsBarnetillegg
                } else {
                    null
                },
            )
        }
    }

    suspend fun hentPerioder(fnr: Fnr, periode: Periode): List<PeriodisertKilde> {
        val dto = hentPerioder(
            ArenaRequestDTO(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        ) ?: return emptyList()

        return dto.map {
            PeriodisertKilde(
                periode = Periode(
                    it.fraOgMed,
                    it.tilOgMed ?: LocalDate.of(9999, 12, 31),
                ),
                kilde = Kilde.ARENA,
            )
        }
    }

    private suspend fun hentVedtak(req: ArenaRequestDTO): List<ArenaResponseDTO>? {
        try {
            val httpResponse =
                httpClient.post("$baseUrl/azure/tiltakspenger/vedtaksperioder") {
                    header(NAV_CALL_ID_HEADER, NAV_CALL_ID_HEADER)
                    bearerAuth(getToken().token)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    Sikkerlogg.info { "hentet vedtak fra Arena for ident ${req.ident}" }
                    return httpResponse.call.response.body()
                }

                else -> {
                    log.error { "Kallet til tiltakspenger-arena feilet ${httpResponse.status} ${httpResponse.status.description}" }
                    throw KallTilVedtakFeilException("Kallet til tiltakspenger-arena feilet ${httpResponse.status} ${httpResponse.status.description}")
                }
            }
        } catch (throwable: Throwable) {
            log.warn { "Uhåndtert feil mot tiltakspenger-arena. Mottat feilmelding ${throwable.message}" }
            throw KallTilVedtakFeilException("Uhåndtert feil mot tiltakspenger-arena. Mottat feilmelding ${throwable.message}")
        }
    }

    private suspend fun hentPerioder(req: ArenaRequestDTO): List<ArenaPeriodeResponseDTO>? {
        try {
            val httpResponse =
                httpClient.post("$baseUrl/azure/tiltakspenger/rettighetsperioder") {
                    header(NAV_CALL_ID_HEADER, NAV_CALL_ID_HEADER)
                    bearerAuth(getToken().token)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    Sikkerlogg.info { "hentet perioder fra Arena for ident ${req.ident}" }
                    return httpResponse.call.response.body()
                }

                else -> {
                    log.error { "Kallet til tiltakspenger-arena perioder feilet ${httpResponse.status} ${httpResponse.status.description}" }
                    throw KallTilVedtakFeilException("Kallet til tiltakspenger-arena perioder feilet ${httpResponse.status} ${httpResponse.status.description}")
                }
            }
        } catch (throwable: Throwable) {
            log.warn { "Uhåndtert feil mot tiltakspenger-arena perioder. Mottat feilmelding ${throwable.message}" }
            throw KallTilVedtakFeilException("Uhåndtert feil mot tiltakspenger-arena perioder. Mottat feilmelding ${throwable.message}")
        }
    }
}
