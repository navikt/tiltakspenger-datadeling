package no.nav.tiltakspenger.datadeling.arena.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.datadeling.Kilde
import no.nav.tiltakspenger.datadeling.arena.ArenaAnmerkning
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikkDetaljer
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtakfakta
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.datadeling.testutils.token
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

class ArenaClientTest {
    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = token
    }

    /**
     * Bygger den ekte klienten over en fake transport: hele klient-pipelinen kjører, kun nettverket byttes ut.
     */
    private fun arenaClient(transport: FakeHttpTransport): ArenaClient = ArenaHttpClient(
        baseUrl = baseUrl,
        clock = fixedClock,
        authTokenProvider = authTokenProvider,
        transport = transport,
    )

    private val baseUrl = "http://arena"

    private val ident = "01834567891"
    private val fnr = Fnr.fromString(ident)
    private val periode = Periode(LocalDate.parse("2022-01-01"), LocalDate.parse("2022-12-31"))

    @Test
    fun `hent av vedtak fra arena`() {
        val beslutningsdato = LocalDate.parse("2022-01-05")
        val sakOpprettetDato = LocalDate.parse("2022-01-01")

        fun vedtakJson(rettighet: String) = """
            {
              "fraOgMed": "${periode.fraOgMed}",
              "tilOgMed": "${periode.tilOgMed}",
              "antallDager": 10.0,
              "dagsatsTiltakspenger": 285,
              "dagsatsBarnetillegg": 55,
              "antallBarn": 1,
              "relaterteTiltak": "tiltak",
              "rettighet": "$rettighet",
              "vedtakId": 36475317,
              "sakId": 13297369,
              "beslutningsdato": "$beslutningsdato",
              "sak": {
                "saksnummer": "202229331",
                "opprettetDato": "$sakOpprettetDato",
                "status": "Aktiv"
              }
            }
        """.trimIndent()

        fun forventetVedtak(rettighet: Rettighet, dagsatsTiltakspenger: Int?, dagsatsBarnetillegg: Int?) = ArenaVedtak(
            periode = periode,
            rettighet = rettighet,
            vedtakId = "36475317",
            kilde = Kilde.ARENA,
            fnr = fnr,
            antallBarn = 1,
            dagsatsTiltakspenger = dagsatsTiltakspenger,
            dagsatsBarnetillegg = dagsatsBarnetillegg,
            beslutningsdato = beslutningsdato,
            sak = ArenaVedtak.Sak(
                sakId = "13297369",
                saksnummer = "202229331",
                opprettetDato = sakOpprettetDato,
                status = "Aktiv",
            ),
        )

        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                """
                [
                  ${vedtakJson("TILTAKSPENGER")},
                  ${vedtakJson("BARNETILLEGG")},
                  ${vedtakJson("TILTAKSPENGER_OG_BARNETILLEGG")},
                  ${vedtakJson("INGENTING")}
                ]
                """.trimIndent(),
            )
        }

        runTest {
            val respons = arenaClient(transport).hentVedtak(fnr, periode).getOrFail()

            respons.body shouldBe listOf(
                forventetVedtak(Rettighet.TILTAKSPENGER, dagsatsTiltakspenger = 285, dagsatsBarnetillegg = null),
                forventetVedtak(Rettighet.BARNETILLEGG, dagsatsTiltakspenger = null, dagsatsBarnetillegg = 55),
                forventetVedtak(Rettighet.TILTAKSPENGER_OG_BARNETILLEGG, dagsatsTiltakspenger = 285, dagsatsBarnetillegg = 55),
                forventetVedtak(Rettighet.INGENTING, dagsatsTiltakspenger = null, dagsatsBarnetillegg = null),
            )
            // Rå request (til sikkerlogg) har med ident og maskert Authorization-header.
            respons.rawRequestString shouldContain """"ident":"$ident""""
            respons.rawRequestString shouldContain "Authorization: ***"
            respons.rawRequestString shouldNotContain token.token
        }
    }

    @Test
    fun `hent av meldekort fra arena`() {
        //language=JSON
        val responseJson = """
            [
              {
                "meldekortId": "1537779132",
                "mottatt": "2020-08-31",
                "arbeidet": false,
                "kurs": true,
                "ferie": false,
                "syk": false,
                "annetFravaer": false,
                "registrert": "2020-08-20T20:00:27",
                "sistEndret": "2021-02-24T20:10:10",
                "type": "Manuelt - Korrigering",
                "status": "Beregning utført",
                "statusDato": "2021-02-24",
                "meldegruppe": "Flere meldegrupper",
                "aar": 2020,
                "totaltArbeidetTimer": 0,
                "periode": {
                  "aar": 2020,
                  "periodekode": 34,
                  "ukenrUke1": 34,
                  "ukenrUke2": 35,
                  "fraOgMed": "2020-08-17",
                  "tilOgMed": "2020-08-30"
                },
                "dager": [
                  {
                    "ukeNr": 34,
                    "dagNr": 1,
                    "arbeidsdag": false,
                    "ferie": null,
                    "kurs": true,
                    "syk": false,
                    "annetFravaer": false,
                    "registrertAv": "GRENSESN",
                    "registrert": "2021-02-24T08:10:35",
                    "arbeidetTimer": 0
                  },
                  {
                    "ukeNr": 34,
                    "dagNr": 2,
                    "arbeidsdag": true,
                    "ferie": false,
                    "kurs": false,
                    "syk": false,
                    "annetFravaer": false,
                    "registrertAv": "GRENSESN",
                    "registrert": "2021-02-24T08:10:35",
                    "arbeidetTimer": 8
                  }
                ],
                "fortsattArbeidsoker": true
              }
            ]
        """.trimIndent()

        val transport = FakeHttpTransport().apply { leggIKøJson(responseJson) }

        runTest {
            val respons = arenaClient(transport).hentMeldekort(
                ArenaClient.ArenaForespørsel(ident = ident, fom = periode.fraOgMed, tom = periode.tilOgMed),
            ).getOrFail()

            respons.body shouldBe listOf(
                ArenaMeldekort(
                    meldekortId = "1537779132",
                    mottatt = LocalDate.parse("2020-08-31"),
                    arbeidet = false,
                    kurs = true,
                    ferie = false,
                    syk = false,
                    annetFravaer = false,
                    fortsattArbeidsoker = true,
                    registrert = LocalDateTime.parse("2020-08-20T20:00:27"),
                    sistEndret = LocalDateTime.parse("2021-02-24T20:10:10"),
                    type = "Manuelt - Korrigering",
                    status = "Beregning utført",
                    statusDato = LocalDate.parse("2021-02-24"),
                    meldegruppe = "Flere meldegrupper",
                    aar = 2020,
                    totaltArbeidetTimer = 0,
                    periode = ArenaMeldekort.ArenaMeldekortPeriode(
                        aar = 2020,
                        periodekode = 34,
                        ukenrUke1 = 34,
                        ukenrUke2 = 35,
                        fraOgMed = LocalDate.parse("2020-08-17"),
                        tilOgMed = LocalDate.parse("2020-08-30"),
                    ),
                    dager = listOf(
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 34,
                            dagNr = 1,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = true,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 34,
                            dagNr = 2,
                            arbeidsdag = true,
                            ferie = false,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 8,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `hent av utbetalingshistorikk fra arena`() {
        //language=JSON
        val responseJson = """
            [
              {
                "meldekortId": 1537779132,
                "dato": "2021-02-24",
                "transaksjonstype": "UTBETALING",
                "sats": 285.0,
                "status": "UTFØRT",
                "vedtakId": 36475317,
                "belop": 3990.0,
                "fraOgMedDato": "2021-03-01",
                "tilOgMedDato": "2021-03-14"
              },
              {
                "meldekortId": null,
                "dato": "2021-03-10",
                "transaksjonstype": "ETTERBETALING",
                "sats": 0.0,
                "status": "SIMULERT",
                "vedtakId": null,
                "belop": 0.0,
                "fraOgMedDato": "2021-03-01",
                "tilOgMedDato": "2021-03-14"
              }
            ]
        """.trimIndent()

        val transport = FakeHttpTransport().apply { leggIKøJson(responseJson) }

        runTest {
            val respons = arenaClient(transport).hentUtbetalingshistorikk(
                ArenaClient.ArenaForespørsel(ident = ident, fom = periode.fraOgMed, tom = periode.tilOgMed),
            ).getOrFail()

            respons.body shouldBe listOf(
                ArenaUtbetalingshistorikk(
                    meldekortId = 1537779132L,
                    dato = LocalDate.parse("2021-02-24"),
                    transaksjonstype = "UTBETALING",
                    sats = 285.0,
                    status = "UTFØRT",
                    vedtakId = 36475317L,
                    belop = 3990.0,
                    fraOgMedDato = LocalDate.parse("2021-03-01"),
                    tilOgMedDato = LocalDate.parse("2021-03-14"),
                ),
                ArenaUtbetalingshistorikk(
                    meldekortId = null,
                    dato = LocalDate.parse("2021-03-10"),
                    transaksjonstype = "ETTERBETALING",
                    sats = 0.0,
                    status = "SIMULERT",
                    vedtakId = null,
                    belop = 0.0,
                    fraOgMedDato = LocalDate.parse("2021-03-01"),
                    tilOgMedDato = LocalDate.parse("2021-03-14"),
                ),
            )
        }
    }

    @Test
    fun `hent av utbetalingshistorikk detaljer fra arena`() {
        //language=JSON
        val responseJson = """
            {
              "vedtakfakta": {
                "dagsats": 285,
                "gjelderFra": "2021-03-01",
                "gjelderTil": "2021-03-14",
                "antallUtbetalinger": 2,
                "belopPerUtbetalinger": 1995,
                "alternativBetalingsmottaker": null
              },
              "anmerkninger": [
                {
                  "kilde": "Meldekort",
                  "registrert": "2021-03-15T10:11:12",
                  "beskrivelse": "Noe ble endret"
                }
              ]
            }
        """.trimIndent()

        val transport = FakeHttpTransport().apply { leggIKøJson(responseJson) }

        runTest {
            val respons = arenaClient(transport).hentUtbetalingshistorikkDetaljer(
                ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel(
                    vedtakId = 36475317L,
                    meldekortId = 1537779132L,
                ),
            ).getOrFail()

            respons.body shouldBe ArenaUtbetalingshistorikkDetaljer(
                vedtakfakta = ArenaVedtakfakta(
                    dagsats = 285,
                    gjelderFra = LocalDate.parse("2021-03-01"),
                    gjelderTil = LocalDate.parse("2021-03-14"),
                    antallUtbetalinger = 2,
                    belopPerUtbetalinger = 1995,
                    alternativBetalingsmottaker = null,
                ),
                anmerkninger = listOf(
                    ArenaAnmerkning(
                        kilde = "Meldekort",
                        registrert = LocalDateTime.parse("2021-03-15T10:11:12"),
                        beskrivelse = "Noe ble endret",
                    ),
                ),
            )
            // WireMock-stubben matchet tidligere på hele URL-en; her står forventningen eksplisitt.
            transport.mottatteKall.single().uri.toString() shouldBe
                "$baseUrl/azure/tiltakspenger/utbetalingshistorikk/detaljer?vedtakId=36475317&meldekortId=1537779132"
        }
    }

    @Test
    fun `hent av utbetalingshistorikk detaljer uten id-er sender ingen query-parametre`() {
        val transport = FakeHttpTransport().apply { leggIKøJson("""{"vedtakfakta": null, "anmerkninger": []}""") }

        runTest {
            val respons = arenaClient(transport).hentUtbetalingshistorikkDetaljer(
                ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel(vedtakId = null, meldekortId = null),
            ).getOrFail()

            respons.body shouldBe ArenaUtbetalingshistorikkDetaljer(vedtakfakta = null, anmerkninger = emptyList())
            transport.mottatteKall.single().uri.toString() shouldBe "$baseUrl/azure/tiltakspenger/utbetalingshistorikk/detaljer"
        }
    }

    @Test
    fun `feil fra arena gir UventetStatus med status og responsbody`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 500, body = """For input string: "0.961538461538462"""", contentType = "text/plain")
        }

        runTest {
            val feil = arenaClient(transport).hentVedtak(fnr, periode)
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value

            val uventetStatus = feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
            uventetStatus.statusCode shouldBe 500
            uventetStatus.body shouldBe """For input string: "0.961538461538462""""
        }
    }

    @Test
    fun `transportfeil gir IngenRespons`() {
        runTest {
            val fakeTransport = FakeHttpTransport().apply {
                leggIKøKast(IOException("connection reset"))
            }
            val arenaClient = ArenaHttpClient(
                baseUrl = "http://arena",
                clock = fixedClock,
                authTokenProvider = authTokenProvider,
                transport = fakeTransport,
            )

            val feil = arenaClient.hentVedtak(fnr, periode)
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value

            feil.shouldBeInstanceOf<HttpKlientError.IngenRespons>()
        }
    }

    /**
     * Dekker default-verdien for `transport`, altså produksjonsoppkoblingen.
     * De øvrige testene sender inn `FakeHttpTransport`, så uten denne ville linja stått udekket.
     * Å bygge klienten rører ingenting på nettverket.
     */
    @Test
    fun `kan bygges med produksjonstransporten som default`() {
        ArenaHttpClient(
            baseUrl = baseUrl,
            clock = fixedClock,
            authTokenProvider = authTokenProvider,
        ).shouldBeInstanceOf<ArenaClient>()
    }

    @Test
    fun `toString på ArenaForespørsel maskerer ident`() {
        val req = ArenaClient.ArenaForespørsel(
            ident = ident,
            fom = periode.fraOgMed,
            tom = periode.tilOgMed,
        )

        req.toString() shouldBe "ArenaForespørsel(ident=***********, fom=2022-01-01, tom=2022-12-31)"
        req.toString() shouldNotContain ident
    }
}
