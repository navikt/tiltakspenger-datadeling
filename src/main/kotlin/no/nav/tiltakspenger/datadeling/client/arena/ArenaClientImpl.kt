package no.nav.tiltakspenger.datadeling.client.arena

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.Configuration
import no.nav.tiltakspenger.datadeling.auth.defaultHttpClient
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.felles.app.exception.egendefinerteFeil.KallTilVedtakFeilException
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

val log = KotlinLogging.logger {}

class ArenaClientImpl(
    private val config: Configuration.ClientConfig = Configuration.arenaClientConfig(),
    private val getToken: suspend () -> AccessToken,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : ArenaClient {
    companion object {
        const val navCallIdHeader = "tiltakspenger-datadeling"
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

    override suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak> {
        val dto = hentVedtak(
            ArenaRequestDTO(
                ident = ident,
                fom = fom,
                tom = tom,
            ),
        ) ?: return emptyList()

        return dto.map {
            Vedtak(
                fom = it.fraOgMed,
                tom = it.tilOgMed ?: LocalDate.of(9999, 12, 31),
                antallDager = it.antallDager,
                dagsatsTiltakspenger = it.dagsatsTiltakspenger,
                dagsatsBarnetillegg = it.dagsatsBarnetillegg,
                antallBarn = it.antallBarn,
                tiltaksgjennomføringId = it.relaterteTiltak,
                rettighet = when (it.rettighet) {
                    RettighetDTO.TILTAKSPENGER -> no.nav.tiltakspenger.datadeling.domene.Rettighet.TILTAKSPENGER
                    RettighetDTO.BARNETILLEGG -> no.nav.tiltakspenger.datadeling.domene.Rettighet.BARNETILLEGG
                    RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG -> no.nav.tiltakspenger.datadeling.domene.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
                    RettighetDTO.INGENTING -> no.nav.tiltakspenger.datadeling.domene.Rettighet.INGENTING
                },
                vedtakId = it.vedtakId.toString(),
                sakId = it.sakId.toString(),
                saksnummer = null,
                kilde = "arena",
                fnr = Fnr.fromString(ident),
            )
        }
    }

    override suspend fun hentPerioder(ident: String, fom: LocalDate, tom: LocalDate): List<PeriodisertKilde> {
        val dto = hentPerioder(
            ArenaRequestDTO(
                ident = ident,
                fom = fom,
                tom = tom,
            ),
        ) ?: return emptyList()

        return dto.map {
            PeriodisertKilde(
                periode = Periode(
                    it.fraOgMed,
                    it.tilOgMed ?: LocalDate.of(9999, 12, 31),
                ),
                kilde = "arena",
            )
        }
    }

    private suspend fun hentVedtak(req: ArenaRequestDTO): List<ArenaResponseDTO>? {
        try {
            val httpResponse =
                httpClient.post("${config.baseUrl}/azure/tiltakspenger/vedtaksperioder") {
                    header(navCallIdHeader, navCallIdHeader)
                    bearerAuth(getToken().token)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    sikkerlogg.info("hentet vedtak fra Arena for ident ${req.ident}")
                    return httpResponse.call.response.body()
                }

                else -> {
                    log.error("Kallet til tiltakspenger-arena feilet ${httpResponse.status} ${httpResponse.status.description}")
                    throw KallTilVedtakFeilException("Kallet til tiltakspenger-arena feilet ${httpResponse.status} ${httpResponse.status.description}")
                }
            }
        } catch (throwable: Throwable) {
            log.warn("Uhåndtert feil mot tiltakspenger-arena. Mottat feilmelding ${throwable.message}")
            throw KallTilVedtakFeilException("Uhåndtert feil mot tiltakspenger-arena. Mottat feilmelding ${throwable.message}")
        }
    }

    private suspend fun hentPerioder(req: ArenaRequestDTO): List<ArenaPeriodeResponseDTO>? {
        try {
            val httpResponse =
                httpClient.post("${config.baseUrl}/azure/tiltakspenger/rettighetsperioder") {
                    header(navCallIdHeader, navCallIdHeader)
                    bearerAuth(getToken().token)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    sikkerlogg.info("hentet perioder fra Arena for ident ${req.ident}")
                    return httpResponse.call.response.body()
                }

                else -> {
                    log.error("Kallet til tiltakspenger-arena perioder feilet ${httpResponse.status} ${httpResponse.status.description}")
                    throw KallTilVedtakFeilException("Kallet til tiltakspenger-arena perioder feilet ${httpResponse.status} ${httpResponse.status.description}")
                }
            }
        } catch (throwable: Throwable) {
            log.warn("Uhåndtert feil mot tiltakspenger-arena perioder. Mottat feilmelding ${throwable.message}")
            throw KallTilVedtakFeilException("Uhåndtert feil mot tiltakspenger-arena perioder. Mottat feilmelding ${throwable.message}")
        }
    }
}
