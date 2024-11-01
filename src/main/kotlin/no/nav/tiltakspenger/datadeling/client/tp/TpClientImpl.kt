package no.nav.tiltakspenger.datadeling.client.tp

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
import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.felles.app.exception.egendefinerteFeil.KallTilVedtakFeilException
import no.nav.tiltakspenger.datadeling.felles.app.sikkerlogg
import no.nav.tiltakspenger.datadeling.felles.infra.json.objectMapper
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

val log = KotlinLogging.logger {}

class TpClientImpl(
    private val config: Configuration.ClientConfig = Configuration.vedtakClientConfig(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : TpClient {
    companion object {
        const val navCallIdHeader = "tiltakspenger-datadeling"
        const val behandlingPath = "datadeling/behandlinger"
        const val vedtakPerioderPath = "datadeling/vedtak/perioder"
        const val vedtakDetaljerPath = "datadeling/vedtak/detaljer"
    }

    data class TpVedtakPeriodeDTO(
        val vedtakId: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class TpVedtakDetaljerDTO(
        val vedtakId: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val antallDager: Double,
        val dagsatsTiltakspenger: Int,
        val dagsatsBarnetillegg: Int,
        val antallBarn: Int,
        val relaterteTiltak: String,
        val rettighet: TpRettighet,
        val sakId: String,
        val saksnummer: String,
    )

    enum class TpRettighet {
        TILTAKSPENGER,
        BARNETILLEGG,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }

    data class TpBehandlingDTO(
        val behandlingId: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class TpRequestDTO(
        val ident: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    override suspend fun hentBehandlinger(ident: String, fom: LocalDate, tom: LocalDate): List<Behandling> {
        val dto: List<TpBehandlingDTO> = hent(TpRequestDTO(ident, fom, tom), behandlingPath) ?: return emptyList()

        return dto.map {
            Behandling(
                behandlingId = it.behandlingId,
                fom = it.fom,
                tom = it.tom,
            )
        }
    }

    override suspend fun hentVedtakPerioder(ident: String, fom: LocalDate, tom: LocalDate): List<PeriodisertKilde> {
        val dto: List<TpVedtakPeriodeDTO> =
            hent(TpRequestDTO(ident, fom, tom), vedtakPerioderPath) ?: return emptyList()

        return dto.map {
            PeriodisertKilde(
                periode = Periode(it.fom, it.tom),
                kilde = "tp",
            )
        }
    }

    override suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak> {
        val dto: List<TpVedtakDetaljerDTO> =
            hent(TpRequestDTO(ident, fom, tom), vedtakDetaljerPath) ?: return emptyList()

        return dto.map {
            Vedtak(
                fom = it.fom,
                tom = it.tom,
                antallDager = it.antallDager,
                dagsatsTiltakspenger = it.dagsatsTiltakspenger,
                dagsatsBarnetillegg = it.dagsatsBarnetillegg,
                antallBarn = it.antallBarn,
                tiltaksgjennomføringId = it.relaterteTiltak,
                rettighet = when (it.rettighet) {
                    TpRettighet.TILTAKSPENGER -> Rettighet.TILTAKSPENGER
                    TpRettighet.BARNETILLEGG -> Rettighet.BARNETILLEGG
                    TpRettighet.TILTAKSPENGER_OG_BARNETILLEGG -> Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
                    TpRettighet.INGENTING -> Rettighet.INGENTING
                },
                vedtakId = it.vedtakId,
                sakId = it.sakId,
                saksnummer = it.saksnummer,
                kilde = "tp",
                fnr = Fnr.fromString(ident),
            )
        }
    }

    private suspend inline fun <reified T> hent(req: TpRequestDTO, path: String): List<T>? {
        try {
            val httpResponse =
                httpClient.post("${config.baseUrl}/$path") {
                    header(navCallIdHeader, navCallIdHeader)
                    bearerAuth(getToken())
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    sikkerlogg.info("vedtak hentet for ident ${req.ident}")
                    return httpResponse.call.response.body()
                }

                else -> {
                    log.error("Kallet til tiltakspenger-saksbehandling-api feilet ${httpResponse.status} ${httpResponse.status.description}")
                    throw KallTilVedtakFeilException("Kallet til tiltakspenger-saksbehandling-api feilet ${httpResponse.status} ${httpResponse.status.description}")
                }
            }
        } catch (throwable: Throwable) {
            log.warn("Uhåndtert feil mot tiltakspenger-saksbehandling-api. Mottat feilmelding ${throwable.message}")
            throw KallTilVedtakFeilException("Uhåndtert feil mot tiltakspenger-saksbehandling-api. Mottat feilmelding ${throwable.message}")
        }
    }
}
