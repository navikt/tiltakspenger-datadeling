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

    data class ArenaResponseDTO(
        val id: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class ArenaRequestDTO(
        val ident: String,
//        val fom: LocalDate,
//        val tom: LocalDate,
    )

    override suspend fun hent(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak> {
        val dto = hent(ArenaRequestDTO(ident)) ?: return emptyList()

        return dto.map {
            Vedtak(
                fom = it.fom,
                tom = it.tom,
                antallDager = 0.0,
                dagsatsTiltakspenger = 0,
                dagsatsBarnetillegg = 0,
                antallBarn = 0,
            )
        }
    }

    private suspend fun hent(req: ArenaRequestDTO): List<ArenaResponseDTO>? {
        try {
            val httpResponse =
                httpClient.post("${config.baseUrl}/tiltakspengerUten") {
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
