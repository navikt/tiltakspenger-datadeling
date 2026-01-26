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
import no.nav.tiltakspenger.datadeling.application.exception.egendefinerteFeil.KallTilVedtakFeilException
import no.nav.tiltakspenger.datadeling.application.http.httpClientCIO
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaVedtak
import no.nav.tiltakspenger.datadeling.client.arena.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.client.arena.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime

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
        val beslutningsdato: LocalDate?,
        val sak: Sak,
    ) {
        data class Sak(
            val saksnummer: String,
            val opprettetDato: LocalDate,
            val status: String,
        )
    }

    private data class ArenaMeldekortResponseDTO(
        val meldekortId: String,
        val mottatt: LocalDate?,
        val arbeidet: Boolean,
        val kurs: Boolean,
        val ferie: Boolean,
        val syk: Boolean,
        val annetFravaer: Boolean,
        val fortsattArbeidsoker: Boolean,
        val registrert: LocalDateTime,
        val sistEndret: LocalDateTime,
        val type: String,
        val status: String,
        val statusDato: LocalDate,
        val meldegruppe: String,
        val aar: Int,
        val totaltArbeidetTimer: Int,
        val periode: ArenaMeldekortPeriodeResponseDTO,
        val dager: List<ArenaMeldekortDagResponseDTO>,
    ) {
        data class ArenaMeldekortPeriodeResponseDTO(
            val aar: Int,
            val periodekode: Int,
            val ukenrUke1: Int,
            val ukenrUke2: Int,
            val fraOgMed: LocalDate,
            val tilOgMed: LocalDate,
        )

        class ArenaMeldekortDagResponseDTO(
            val ukeNr: Int,
            val dagNr: Int,
            val arbeidsdag: Boolean,
            val ferie: Boolean?,
            val kurs: Boolean,
            val syk: Boolean,
            val annetFravaer: Boolean,
            val registrertAv: String,
            val registrert: LocalDateTime,
            val arbeidetTimer: Int,
        )
    }

    private data class ArenaUtbetalingshistorikkResponseDTO(
        val meldekortId: Long?,
        val dato: LocalDate,
        val transaksjonstype: String,
        val sats: Double,
        val status: String,
        val vedtakId: Long?,
        val belop: Double,
        val fraOgMedDato: LocalDate,
        val tilOgMedDato: LocalDate,
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

    suspend fun hentVedtak(fnr: Fnr, periode: Periode): List<ArenaVedtak> {
        val dto = hentVedtak(
            ArenaRequestDTO(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        ) ?: return emptyList()

        return dto.map {
            ArenaVedtak(
                periode = Periode(it.fraOgMed, it.tilOgMed ?: LocalDate.of(9999, 12, 31)),
                rettighet = when (it.rettighet) {
                    RettighetDTO.TILTAKSPENGER -> Rettighet.TILTAKSPENGER
                    RettighetDTO.BARNETILLEGG -> Rettighet.BARNETILLEGG
                    RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG -> Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
                    RettighetDTO.INGENTING -> Rettighet.INGENTING
                },
                vedtakId = it.vedtakId.toString(),
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
                beslutningsdato = it.beslutningsdato,
                sak = ArenaVedtak.Sak(
                    sakId = it.sakId.toString(),
                    saksnummer = it.sak.saksnummer,
                    opprettetDato = it.sak.opprettetDato,
                    status = it.sak.status,
                ),
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
            log.warn { "Uhåndtert feil mot tiltakspenger-arena. Mottatt feilmelding ${throwable.message}" }
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
            log.warn { "Uhåndtert feil mot tiltakspenger-arena perioder. Mottatt feilmelding ${throwable.message}" }
            throw KallTilVedtakFeilException("Uhåndtert feil mot tiltakspenger-arena perioder. Mottat feilmelding ${throwable.message}")
        }
    }

    suspend fun hentMeldekort(req: ArenaRequestDTO): List<ArenaMeldekort> {
        try {
            val httpResponse =
                httpClient.post("$baseUrl/azure/tiltakspenger/meldekort") {
                    header(NAV_CALL_ID_HEADER, NAV_CALL_ID_HEADER)
                    bearerAuth(getToken().token)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    Sikkerlogg.info { "hentet meldekort fra Arena for ident ${req.ident}" }
                    return (httpResponse.call.response.body() as List<ArenaMeldekortResponseDTO>).map {
                        ArenaMeldekort(
                            meldekortId = it.meldekortId,
                            mottatt = it.mottatt,
                            arbeidet = it.arbeidet,
                            kurs = it.kurs,
                            ferie = it.ferie,
                            syk = it.syk,
                            annetFravaer = it.annetFravaer,
                            fortsattArbeidsoker = it.fortsattArbeidsoker,
                            registrert = it.registrert,
                            sistEndret = it.sistEndret,
                            type = it.type,
                            status = it.status,
                            statusDato = it.statusDato,
                            meldegruppe = it.meldegruppe,
                            aar = it.aar,
                            totaltArbeidetTimer = it.totaltArbeidetTimer,
                            periode = ArenaMeldekort.ArenaMeldekortPeriode(
                                aar = it.periode.aar,
                                periodekode = it.periode.periodekode,
                                ukenrUke1 = it.periode.ukenrUke1,
                                ukenrUke2 = it.periode.ukenrUke2,
                                fraOgMed = it.periode.fraOgMed,
                                tilOgMed = it.periode.tilOgMed,
                            ),
                            dager = it.dager.map { dag ->
                                ArenaMeldekort.ArenaMeldekortDag(
                                    ukeNr = dag.ukeNr,
                                    dagNr = dag.dagNr,
                                    arbeidsdag = dag.arbeidsdag,
                                    ferie = dag.ferie,
                                    kurs = dag.kurs,
                                    syk = dag.syk,
                                    annetFravaer = dag.annetFravaer,
                                    registrertAv = dag.registrertAv,
                                    registrert = dag.registrert,
                                    arbeidetTimer = dag.arbeidetTimer,
                                )
                            },
                        )
                    }
                }

                else -> {
                    log.error { "Kallet til tiltakspenger-arena meldekort feilet ${httpResponse.status} ${httpResponse.status.description}" }
                    throw KallTilVedtakFeilException("Kallet til tiltakspenger-arena meldekort feilet ${httpResponse.status} ${httpResponse.status.description}")
                }
            }
        } catch (throwable: Throwable) {
            log.warn { "Uhåndtert feil mot tiltakspenger-arena meldekort. Mottatt feilmelding ${throwable.message}" }
            throw KallTilVedtakFeilException("Uhåndtert feil mot tiltakspenger-arena meldekort. Mottat feilmelding ${throwable.message}")
        }
    }

    suspend fun hentUtbetalingshistorikk(req: ArenaRequestDTO): List<ArenaUtbetalingshistorikk> {
        try {
            val httpResponse =
                httpClient.post("$baseUrl/azure/tiltakspenger/utbetalingshistorikk") {
                    header(NAV_CALL_ID_HEADER, NAV_CALL_ID_HEADER)
                    bearerAuth(getToken().token)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }

            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    Sikkerlogg.info { "hentet utbetalingshistorikk fra Arena for ident ${req.ident}" }
                    return (httpResponse.call.response.body() as List<ArenaUtbetalingshistorikkResponseDTO>).map {
                        ArenaUtbetalingshistorikk(
                            meldekortId = it.meldekortId,
                            dato = it.dato,
                            transaksjonstype = it.transaksjonstype,
                            sats = it.sats,
                            status = it.status,
                            vedtakId = it.vedtakId,
                            belop = it.belop,
                            fraOgMedDato = it.fraOgMedDato,
                            tilOgMedDato = it.tilOgMedDato,
                        )
                    }
                }

                else -> {
                    log.error { "Kallet til tiltakspenger-arena utbetalingshistorikk feilet ${httpResponse.status} ${httpResponse.status.description}" }
                    throw KallTilVedtakFeilException("Kallet til tiltakspenger-arena utbetalingshistorikk feilet ${httpResponse.status} ${httpResponse.status.description}")
                }
            }
        } catch (throwable: Throwable) {
            log.warn { "Uhåndtert feil mot tiltakspenger-arena meldekort. Mottatt feilmelding ${throwable.message}" }
            throw KallTilVedtakFeilException("Uhåndtert feil mot tiltakspenger-arena utbetalingshistorikk. Mottat feilmelding ${throwable.message}")
        }
    }
}
