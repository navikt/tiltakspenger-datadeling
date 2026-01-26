package no.nav.tiltakspenger.datadeling.client

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.datadeling.application.http.httpClientGeneric
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaVedtak
import no.nav.tiltakspenger.datadeling.client.arena.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.client.arena.domene.Rettighet.TILTAKSPENGER
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.testutils.token
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class ArenaClientTest {
    private fun arenaClient(response: String?): ArenaClient {
        return ArenaClient(
            baseUrl = "https://arena",
            getToken = { token },
            httpClient = httpClientGeneric(mockEngine(response!!)),
        )
    }

    private fun mockEngine(response: String): MockEngine {
        return MockEngine {
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
    }

    @Test
    fun `hent av vedtak fra arena`() {
        val ident = "01234567891"
        val fnr = Fnr.fromString(ident)
        val fom = LocalDate.parse("2022-01-01")
        val tom = LocalDate.parse("2022-12-31")
        val periode = Periode(fom, tom)
        val beslutningsdato = LocalDate.parse("2022-01-05")
        val sakOpprettetDato = LocalDate.parse("2022-01-01")
        val responseJson = """
            [
              {
                "fraOgMed": "$fom",
                "tilOgMed": "$tom",
                "antallDager": 10.0,
                "dagsatsTiltakspenger": 285,
                "dagsatsBarnetillegg": 0,
                "antallBarn": 0,
                "relaterteTiltak": "tiltak",
                "rettighet": "TILTAKSPENGER",
                "vedtakId": 36475317,
                "sakId": 13297369,
                "beslutningsdato": "$beslutningsdato",
                "sak": {
                  "saksnummer": "202229331",
                  "opprettetDato": "$sakOpprettetDato",
                  "status": "Aktiv"
                }
              }
            ]
        """.trimIndent()
        val arenaClient = arenaClient(responseJson)

        runTest {
            val result = arenaClient.hentVedtak(fnr, periode)

            result shouldBe listOf(
                ArenaVedtak(
                    periode = periode,
                    rettighet = TILTAKSPENGER,
                    vedtakId = "36475317",
                    kilde = Kilde.ARENA,
                    fnr = fnr,
                    antallBarn = 0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = null,
                    beslutningsdato = beslutningsdato,
                    sak = ArenaVedtak.Sak(
                        sakId = "13297369",
                        saksnummer = "202229331",
                        opprettetDato = sakOpprettetDato,
                        status = "Aktiv",
                    ),
                ),
            )
        }
    }

    @Test
    fun `hent av perioder fra arena`() {
        val ident = "01234567891"
        val fnr = Fnr.fromString(ident)
        val fom = LocalDate.parse("2022-01-01")
        val tom = LocalDate.parse("2022-12-31")
        val periode = Periode(fom, tom)
        val responseJson = """
            [
              {
                "fraOgMed": "$fom",
                "tilOgMed": "$tom"
              }
            ]
        """.trimIndent()
        val arenaClient = arenaClient(responseJson)

        runTest {
            val result = arenaClient.hentPerioder(fnr, periode)

            result shouldBe listOf(
                PeriodisertKilde(periode, Kilde.ARENA),
            )
        }
    }

    @Test
    fun `hent av meldekort fra arena`() {
        val ident = "01234567891"
        val fom = LocalDate.parse("2022-01-01")
        val tom = LocalDate.parse("2022-12-31")
        //language=JSON
        val responseJson =
            """
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
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": false,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      },
                      {
                        "ukeNr": 34,
                        "dagNr": 3,
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": false,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      },
                      {
                        "ukeNr": 34,
                        "dagNr": 4,
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": false,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      },
                      {
                        "ukeNr": 34,
                        "dagNr": 5,
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
                        "dagNr": 6,
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
                        "dagNr": 7,
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": false,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      },
                      {
                        "ukeNr": 35,
                        "dagNr": 1,
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": false,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      },
                      {
                        "ukeNr": 35,
                        "dagNr": 2,
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": false,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      },
                      {
                        "ukeNr": 35,
                        "dagNr": 3,
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": false,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      },
                      {
                        "ukeNr": 35,
                        "dagNr": 4,
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
                        "ukeNr": 35,
                        "dagNr": 5,
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
                        "ukeNr": 35,
                        "dagNr": 6,
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": false,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      },
                      {
                        "ukeNr": 35,
                        "dagNr": 7,
                        "arbeidsdag": false,
                        "ferie": null,
                        "kurs": true,
                        "syk": false,
                        "annetFravaer": false,
                        "registrertAv": "GRENSESN",
                        "registrert": "2021-02-24T08:10:35",
                        "arbeidetTimer": 0
                      }
                    ],
                    "fortsattArbeidsoker": true
                  }
                ]
            """.trimIndent()
        val arenaClient = arenaClient(responseJson)

        runTest {
            val result = arenaClient.hentMeldekort(
                ArenaClient.ArenaRequestDTO(
                    ident = ident,
                    fom = fom,
                    tom = tom,
                ),
            )

            result shouldBe listOf(
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
                            arbeidsdag = false,
                            ferie = null,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 34,
                            dagNr = 3,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 34,
                            dagNr = 4,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 34,
                            dagNr = 5,
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
                            dagNr = 6,
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
                            dagNr = 7,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 35,
                            dagNr = 1,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 35,
                            dagNr = 2,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 35,
                            dagNr = 3,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 35,
                            dagNr = 4,
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
                            ukeNr = 35,
                            dagNr = 5,
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
                            ukeNr = 35,
                            dagNr = 6,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = false,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                        ArenaMeldekort.ArenaMeldekortDag(
                            ukeNr = 35,
                            dagNr = 7,
                            arbeidsdag = false,
                            ferie = null,
                            kurs = true,
                            syk = false,
                            annetFravaer = false,
                            registrertAv = "GRENSESN",
                            registrert = LocalDateTime.parse("2021-02-24T08:10:35"),
                            arbeidetTimer = 0,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `hent av utbetalingshistorikk fra arena`() {
        val ident = "01234567891"
        val fom = LocalDate.parse("2022-01-01")
        val tom = LocalDate.parse("2022-12-31")

        //language=JSON
        val responseJson =
            """
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

        val arenaClient = arenaClient(responseJson)

        runTest {
            val result = arenaClient.hentUtbetalingshistorikk(
                ArenaClient.ArenaRequestDTO(
                    ident = ident,
                    fom = fom,
                    tom = tom,
                ),
            )

            result shouldBe listOf(
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
