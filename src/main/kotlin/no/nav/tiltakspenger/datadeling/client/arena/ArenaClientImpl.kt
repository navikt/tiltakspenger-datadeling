package no.nav.tiltakspenger.datadeling.client.arena

import com.fasterxml.jackson.databind.ObjectMapper
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
import no.nav.tiltakspenger.datadeling.auth.defaultObjectMapper
import no.nav.tiltakspenger.datadeling.domene.Periode
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.exception.egendefinerteFeil.KallTilVedtakFeilException
import java.time.LocalDate

val log = KotlinLogging.logger {}
val securelog = KotlinLogging.logger("tjenestekall")

class ArenaClientImpl(
    private val config: Configuration.ClientConfig = Configuration.arenaClientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : ArenaClient {
    companion object {
        const val navCallIdHeader = "tiltakspenger-datadeling"
    }

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
        val dto = hent(
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
                relaterteTiltak = it.relaterteTiltak,
                rettighet = when (it.rettighet) {
                    RettighetDTO.TILTAKSPENGER -> no.nav.tiltakspenger.datadeling.domene.Rettighet.TILTAKSPENGER
                    RettighetDTO.BARNETILLEGG -> no.nav.tiltakspenger.datadeling.domene.Rettighet.BARNETILLEGG
                    RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG -> no.nav.tiltakspenger.datadeling.domene.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
                    RettighetDTO.INGENTING -> no.nav.tiltakspenger.datadeling.domene.Rettighet.INGENTING
                },
                vedtakId = it.vedtakId.toString(),
                sakId = it.sakId.toString(),
            )
        }
    }

    override suspend fun hentPerioder(ident: String, fom: LocalDate, tom: LocalDate): List<Periode> {
        val dto = hent(
            ArenaRequestDTO(
                ident = ident,
                fom = fom,
                tom = tom,
            ),
        ) ?: return emptyList()

        return dto.map {
            Periode(
                fom = it.fraOgMed,
                tom = it.tilOgMed ?: LocalDate.of(9999, 12, 31),
            )
        }
    }

    private suspend fun hent(req: ArenaRequestDTO): List<ArenaResponseDTO>? {
        try {
            val httpResponse =
                httpClient.post("${config.baseUrl}/tiltakspenger/vedtaksperioder") {
                    header(navCallIdHeader, navCallIdHeader)
                    bearerAuth(getToken())
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    securelog.info("hentet vedtak fra Arena for ident ${req.ident}")
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
}
