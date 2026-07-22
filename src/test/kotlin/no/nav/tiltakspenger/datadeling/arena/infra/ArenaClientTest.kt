package no.nav.tiltakspenger.datadeling.arena.infra

import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
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
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

internal class ArenaClientTest {
    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = token
    }

    /** Bygger klienten med default `HttpKlient`-oppsett (reell transport). */
    private fun arenaClient(baseUrl: String): ArenaClient = ArenaHttpClient(
        baseUrl = baseUrl,
        clock = fixedClock,
        authTokenProvider = authTokenProvider,
    )

    private val ident = "01234567891"
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

        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/azure/tiltakspenger/vedtaksperioder"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = """
                    [
                      ${vedtakJson("TILTAKSPENGER")},
                      ${vedtakJson("BARNETILLEGG")},
                      ${vedtakJson("TILTAKSPENGER_OG_BARNETILLEGG")},
                      ${vedtakJson("INGENTING")}
                    ]
                """.trimIndent()
            }

            runTest {
                val respons = arenaClient(wiremock.baseUrl()).hentVedtak(fnr, periode).getOrFail()

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

        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/azure/tiltakspenger/meldekort"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = responseJson
            }

            runTest {
                val respons = arenaClient(wiremock.baseUrl()).hentMeldekort(
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

        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/azure/tiltakspenger/utbetalingshistorikk"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = responseJson
            }

            runTest {
                val respons = arenaClient(wiremock.baseUrl()).hentUtbetalingshistorikk(
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

        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/azure/tiltakspenger/utbetalingshistorikk/detaljer?vedtakId=36475317&meldekortId=1537779132"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = responseJson
            }

            runTest {
                val respons = arenaClient(wiremock.baseUrl()).hentUtbetalingshistorikkDetaljer(
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
            }
        }
    }

    @Test
    fun `hent av utbetalingshistorikk detaljer uten id-er sender ingen query-parametre`() {
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/azure/tiltakspenger/utbetalingshistorikk/detaljer"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = """{"vedtakfakta": null, "anmerkninger": []}"""
            }

            runTest {
                val respons = arenaClient(wiremock.baseUrl()).hentUtbetalingshistorikkDetaljer(
                    ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel(vedtakId = null, meldekortId = null),
                ).getOrFail()

                respons.body shouldBe ArenaUtbetalingshistorikkDetaljer(vedtakfakta = null, anmerkninger = emptyList())
            }
        }
    }

    @Test
    fun `feil fra arena gir UventetStatus med status og responsbody`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/azure/tiltakspenger/vedtaksperioder"
            } returns {
                statusCode = 500
                header = "Content-Type" to "text/plain"
                body = """For input string: "0.961538461538462""""
            }

            runTest {
                val feil = arenaClient(wiremock.baseUrl()).hentVedtak(fnr, periode)
                    .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                    .value

                val uventetStatus = feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
                uventetStatus.statusCode shouldBe 500
                uventetStatus.body shouldBe """For input string: "0.961538461538462""""
            }
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
