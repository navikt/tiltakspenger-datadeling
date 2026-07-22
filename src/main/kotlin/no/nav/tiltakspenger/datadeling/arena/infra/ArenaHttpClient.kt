package no.nav.tiltakspenger.datadeling.arena.infra

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.Kilde
import no.nav.tiltakspenger.datadeling.arena.ArenaAnmerkning
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikkDetaljer
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtakfakta
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.periode.Periode
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot tiltakspenger-arena for å hente vedtak, meldekort og utbetalingshistorikk fra Arena.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-arena
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * Timeoutene på 60 sekunder er arvet fra den gamle ktor-klienten; utbetalingshistorikk-oppslagene mot Arena kan være trege.
 */
class ArenaHttpClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 60.seconds,
    timeout: Duration = 60.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : ArenaClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val vedtaksperioderUri = URI.create("$baseUrl/azure/tiltakspenger/vedtaksperioder")
    private val meldekortUri = URI.create("$baseUrl/azure/tiltakspenger/meldekort")
    private val utbetalingshistorikkUri = URI.create("$baseUrl/azure/tiltakspenger/utbetalingshistorikk")
    private val utbetalingshistorikkDetaljerUri = URI.create("$baseUrl/azure/tiltakspenger/utbetalingshistorikk/detaljer")

    override suspend fun hentVedtak(
        fnr: Fnr,
        periode: Periode,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaVedtak>>> {
        return httpKlient.postJson<List<ArenaResponseDTO>>(
            uri = vedtaksperioderUri,
            body = ArenaForespørselDTO(ident = fnr.verdi, fom = periode.fraOgMed, tom = periode.tilOgMed),
            godta = Statusregel.Eksakt(200),
        ).map { respons -> respons.medBody(respons.body.map { it.toDomain(fnr) }) }
    }

    override suspend fun hentMeldekort(
        req: ArenaClient.ArenaForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaMeldekort>>> {
        return httpKlient.postJson<List<ArenaMeldekortResponseDTO>>(
            uri = meldekortUri,
            body = ArenaForespørselDTO(ident = req.ident, fom = req.fom, tom = req.tom),
            godta = Statusregel.Eksakt(200),
        ).map { respons -> respons.medBody(respons.body.map { it.toDomain() }) }
    }

    override suspend fun hentUtbetalingshistorikk(
        req: ArenaClient.ArenaForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaUtbetalingshistorikk>>> {
        return httpKlient.postJson<List<ArenaUtbetalingshistorikkResponseDTO>>(
            uri = utbetalingshistorikkUri,
            body = ArenaForespørselDTO(ident = req.ident, fom = req.fom, tom = req.tom),
            godta = Statusregel.Eksakt(200),
        ).map { respons -> respons.medBody(respons.body.map { it.toDomain() }) }
    }

    override suspend fun hentUtbetalingshistorikkDetaljer(
        req: ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<ArenaUtbetalingshistorikkDetaljer>> {
        val query = listOfNotNull(
            req.vedtakId?.let { "vedtakId=$it" },
            req.meldekortId?.let { "meldekortId=$it" },
        ).joinToString("&")
        val uri = if (query.isEmpty()) utbetalingshistorikkDetaljerUri else URI.create("$utbetalingshistorikkDetaljerUri?$query")
        return httpKlient.getJson<ArenaUtbetalingshistorikkDetaljerResponseDTO>(
            uri = uri,
            godta = Statusregel.Eksakt(200),
        ).map { respons -> respons.medBody(respons.body.toDomain()) }
    }

    private data class ArenaForespørselDTO(
        val ident: String,
        val fom: LocalDate,
        val tom: LocalDate,
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

        fun toDomain(fnr: Fnr): ArenaVedtak = ArenaVedtak(
            periode = Periode(fraOgMed, tilOgMed ?: LocalDate.of(9999, 12, 31)),
            rettighet = when (rettighet) {
                RettighetDTO.TILTAKSPENGER -> Rettighet.TILTAKSPENGER
                RettighetDTO.BARNETILLEGG -> Rettighet.BARNETILLEGG
                RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG -> Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
                RettighetDTO.INGENTING -> Rettighet.INGENTING
            },
            vedtakId = vedtakId.toString(),
            kilde = Kilde.ARENA,
            fnr = fnr,
            antallBarn = antallBarn,
            dagsatsTiltakspenger = if (rettighet == RettighetDTO.TILTAKSPENGER || rettighet == RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG) {
                dagsatsTiltakspenger
            } else {
                null
            },
            dagsatsBarnetillegg = if (rettighet == RettighetDTO.BARNETILLEGG || rettighet == RettighetDTO.TILTAKSPENGER_OG_BARNETILLEGG) {
                dagsatsBarnetillegg
            } else {
                null
            },
            beslutningsdato = beslutningsdato,
            sak = ArenaVedtak.Sak(
                sakId = sakId.toString(),
                saksnummer = sak.saksnummer,
                opprettetDato = sak.opprettetDato,
                status = sak.status,
            ),
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

        fun toDomain(): ArenaMeldekort = ArenaMeldekort(
            meldekortId = meldekortId,
            mottatt = mottatt,
            arbeidet = arbeidet,
            kurs = kurs,
            ferie = ferie,
            syk = syk,
            annetFravaer = annetFravaer,
            fortsattArbeidsoker = fortsattArbeidsoker,
            registrert = registrert,
            sistEndret = sistEndret,
            type = type,
            status = status,
            statusDato = statusDato,
            meldegruppe = meldegruppe,
            aar = aar,
            totaltArbeidetTimer = totaltArbeidetTimer,
            periode = ArenaMeldekort.ArenaMeldekortPeriode(
                aar = periode.aar,
                periodekode = periode.periodekode,
                ukenrUke1 = periode.ukenrUke1,
                ukenrUke2 = periode.ukenrUke2,
                fraOgMed = periode.fraOgMed,
                tilOgMed = periode.tilOgMed,
            ),
            dager = dager.map { dag ->
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
    ) {
        fun toDomain(): ArenaUtbetalingshistorikk = ArenaUtbetalingshistorikk(
            meldekortId = meldekortId,
            dato = dato,
            transaksjonstype = transaksjonstype,
            sats = sats,
            status = status,
            vedtakId = vedtakId,
            belop = belop,
            fraOgMedDato = fraOgMedDato,
            tilOgMedDato = tilOgMedDato,
        )
    }

    private data class ArenaUtbetalingshistorikkDetaljerResponseDTO(
        val vedtakfakta: ArenaUtbetalingshistorikkVedtakfaktaResponseDTO?,
        val anmerkninger: List<ArenaAnmerkningResponseDTO>,
    ) {
        fun toDomain(): ArenaUtbetalingshistorikkDetaljer = ArenaUtbetalingshistorikkDetaljer(
            vedtakfakta = vedtakfakta?.let {
                ArenaVedtakfakta(
                    dagsats = it.dagsats,
                    gjelderFra = it.gjelderFra,
                    gjelderTil = it.gjelderTil,
                    antallUtbetalinger = it.antallUtbetalinger,
                    belopPerUtbetalinger = it.belopPerUtbetalinger,
                    alternativBetalingsmottaker = it.alternativBetalingsmottaker,
                )
            },
            anmerkninger = anmerkninger.map {
                ArenaAnmerkning(
                    kilde = it.kilde,
                    registrert = it.registrert,
                    beskrivelse = it.beskrivelse,
                )
            },
        )
    }

    private data class ArenaUtbetalingshistorikkVedtakfaktaResponseDTO(
        val dagsats: Int?,
        val gjelderFra: LocalDate?,
        val gjelderTil: LocalDate?,
        val antallUtbetalinger: Int?,
        val belopPerUtbetalinger: Int?,
        val alternativBetalingsmottaker: String?,
    )

    private data class ArenaAnmerkningResponseDTO(
        val kilde: String?,
        val registrert: LocalDateTime?,
        val beskrivelse: String?,
    )

    private enum class RettighetDTO {
        TILTAKSPENGER,
        BARNETILLEGG,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }
}

/** Bevarer status og metadata, bytter body — til DTO → domene-mapping i suksess-grenen. */
private fun <T, R> HttpKlientResponse<T>.medBody(body: R): HttpKlientResponse<R> =
    HttpKlientResponse(statusCode = statusCode, body = body, metadata = metadata)
