package no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.MeldekortService
import no.nav.tiltakspenger.datadeling.testdata.MeldekortMother
import no.nav.tiltakspenger.datadeling.testdata.MeldeperiodeMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.configureTestApplication
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortRoutesTest {
    val sakId = SakId.random()
    val fnr = Fnr.fromString("12345678910")
    val sak = SakMother.sak(
        id = sakId.toString(),
        fnr = fnr,
    )

    @Test
    fun `hent meldekort - har godkjent meldekort, ingen klar til utfylling - returnerer riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
                val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
                sakRepo.lagre(sak)
                val meldeperiode = MeldeperiodeMother.meldeperiode(
                    sakId = sakId,
                    periode = MeldeperiodeMother.periode(
                        fraSisteMandagFor = LocalDate.now().minusDays(14),
                        tilSisteSondagEtter = null,
                    ),
                )
                meldeperiodeRepo.lagre(listOf(meldeperiode))
                val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode)
                godkjentMeldekortRepo.lagre(godkjentMeldekort)
                val meldekortService = MeldekortService(meldeperiodeRepo)
                val token = getGyldigToken()
                testApplication {
                    configureTestApplication(
                        meldekortService = meldekortService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/meldekort/detaljer")
                        },
                        jwt = token,
                    ) {
                        setBody(
                            """
                        {
                            "ident": "12345678910",
                            "fom": "2023-01-01",
                            "tom": null
                        }
                            """.trimIndent(),
                        )
                    }
                        .apply {
                            withClue(
                                "Response details:\n" +
                                    "Status: ${this.status}\n" +
                                    "Content-Type: ${this.contentType()}\n" +
                                    "Body: ${this.bodyAsText()}\n",
                            ) {
                                status shouldBe HttpStatusCode.OK
                                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                                val response = objectMapper.readValue<MeldekortResponse>(bodyAsText())
                                response.meldekortKlareTilUtfylling shouldBe emptyList()
                                response.godkjenteMeldekort.size shouldBe 1
                                val godkjentMeldekortResponse = response.godkjenteMeldekort.first()
                                godkjentMeldekortResponse.meldeperiodeId shouldBe godkjentMeldekort.meldeperiodeId.toString()
                                godkjentMeldekortResponse.fraOgMed shouldBe godkjentMeldekort.fraOgMed
                                godkjentMeldekortResponse.tilOgMed shouldBe godkjentMeldekort.tilOgMed
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent meldekort - har meldekort klart til utfylling, ingen godkjente - returnerer riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
                sakRepo.lagre(sak)
                val meldeperiode = MeldeperiodeMother.meldeperiode(
                    sakId = sakId,
                    periode = MeldeperiodeMother.periode(
                        fraSisteMandagFor = LocalDate.now().minusDays(14),
                        tilSisteSondagEtter = null,
                    ),
                )

                meldeperiodeRepo.lagre(listOf(meldeperiode))

                val meldekortService = MeldekortService(meldeperiodeRepo)
                val token = getGyldigToken()
                testApplication {
                    configureTestApplication(
                        meldekortService = meldekortService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/meldekort/detaljer")
                        },
                        jwt = token,
                    ) {
                        setBody(
                            """
                        {
                            "ident": "12345678910",
                            "fom": "2023-01-01",
                            "tom": null
                        }
                            """.trimIndent(),
                        )
                    }
                        .apply {
                            withClue(
                                "Response details:\n" +
                                    "Status: ${this.status}\n" +
                                    "Content-Type: ${this.contentType()}\n" +
                                    "Body: ${this.bodyAsText()}\n",
                            ) {
                                status shouldBe HttpStatusCode.OK
                                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                                val response = objectMapper.readValue<MeldekortResponse>(bodyAsText())
                                response.meldekortKlareTilUtfylling.size shouldBe 1
                                val meldekortKlartTilUtfylling = response.meldekortKlareTilUtfylling.first()
                                meldekortKlartTilUtfylling.id shouldBe meldeperiode.id.toString()
                                meldekortKlartTilUtfylling.fraOgMed shouldBe meldeperiode.fraOgMed
                                meldekortKlartTilUtfylling.tilOgMed shouldBe meldeperiode.tilOgMed
                                meldekortKlartTilUtfylling.girRett shouldBe meldeperiode.girRett
                                meldekortKlartTilUtfylling.kanFyllesUtFraOgMed shouldBe meldeperiode.tilOgMed.minusDays(2)
                                response.godkjenteMeldekort shouldBe emptyList()
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent meldekort - har godkjent meldekort, et klar til utfylling, et fremtidig - returnerer riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
                val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
                sakRepo.lagre(sak)
                val meldeperiode1 = MeldeperiodeMother.meldeperiode(
                    sakId = sakId,
                    periode = MeldeperiodeMother.periode(
                        fraSisteMandagFor = LocalDate.now().minusDays(28),
                        tilSisteSondagEtter = null,
                    ),
                )
                meldeperiodeRepo.lagre(listOf(meldeperiode1))
                val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode1)
                godkjentMeldekortRepo.lagre(godkjentMeldekort)
                val meldeperiode2 = MeldeperiodeMother.meldeperiode(
                    sakId = sakId,
                    periode = MeldeperiodeMother.periode(
                        fraSisteMandagFor = LocalDate.now().minusDays(14),
                        tilSisteSondagEtter = null,
                    ),
                )
                meldeperiodeRepo.lagre(listOf(meldeperiode2))
                val meldeperiode3 = MeldeperiodeMother.meldeperiode(
                    sakId = sakId,
                )
                meldeperiodeRepo.lagre(listOf(meldeperiode3))
                val meldekortService = MeldekortService(meldeperiodeRepo)
                val token = getGyldigToken()
                testApplication {
                    configureTestApplication(
                        meldekortService = meldekortService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/meldekort/detaljer")
                        },
                        jwt = token,
                    ) {
                        setBody(
                            """
                        {
                            "ident": "12345678910",
                            "fom": "2023-01-01",
                            "tom": null
                        }
                            """.trimIndent(),
                        )
                    }
                        .apply {
                            withClue(
                                "Response details:\n" +
                                    "Status: ${this.status}\n" +
                                    "Content-Type: ${this.contentType()}\n" +
                                    "Body: ${this.bodyAsText()}\n",
                            ) {
                                status shouldBe HttpStatusCode.OK
                                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                                val response = objectMapper.readValue<MeldekortResponse>(bodyAsText())
                                response.meldekortKlareTilUtfylling.size shouldBe 1
                                val meldekortKlartTilUtfylling = response.meldekortKlareTilUtfylling.first()
                                meldekortKlartTilUtfylling.id shouldBe meldeperiode2.id.toString()
                                meldekortKlartTilUtfylling.fraOgMed shouldBe meldeperiode2.fraOgMed
                                meldekortKlartTilUtfylling.tilOgMed shouldBe meldeperiode2.tilOgMed
                                meldekortKlartTilUtfylling.girRett shouldBe meldeperiode2.girRett
                                meldekortKlartTilUtfylling.kanFyllesUtFraOgMed shouldBe meldeperiode2.tilOgMed.minusDays(2)
                                response.godkjenteMeldekort.size shouldBe 1
                                val godkjentMeldekortResponse = response.godkjenteMeldekort.first()
                                godkjentMeldekortResponse.meldeperiodeId shouldBe godkjentMeldekort.meldeperiodeId.toString()
                                godkjentMeldekortResponse.fraOgMed shouldBe godkjentMeldekort.fraOgMed
                                godkjentMeldekortResponse.tilOgMed shouldBe godkjentMeldekort.tilOgMed
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent meldekort - har ingen meldekort - returnerer riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
                val meldekortService = MeldekortService(meldeperiodeRepo)
                val token = getGyldigToken()
                testApplication {
                    configureTestApplication(
                        meldekortService = meldekortService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/meldekort/detaljer")
                        },
                        jwt = token,
                    ) {
                        setBody(
                            """
                        {
                            "ident": "12345678910",
                            "fom": "2023-01-01",
                            "tom": "2024-12-31"
                        }
                            """.trimIndent(),
                        )
                    }
                        .apply {
                            withClue(
                                "Response details:\n" +
                                    "Status: ${this.status}\n" +
                                    "Content-Type: ${this.contentType()}\n" +
                                    "Body: ${this.bodyAsText()}\n",
                            ) {
                                status shouldBe HttpStatusCode.OK
                                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                                bodyAsText().shouldEqualJson(
                                    """
                                        {
                                          "meldekortKlareTilUtfylling": [],
                                          "godkjenteMeldekort": []
                                        }
                                    """.trimIndent(),
                                )
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `post med ugyldig token skal gi 401`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                configureTestApplication(texasClient = tac.texasClient)
                val response = client.post("/meldekort/detaljer") {
                    header("Authorization", "Bearer tulletoken")
                    header("Content-Type", "application/json")
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2023-01-01",
                            "tom": "2024-12-31"
                        }
                        """.trimIndent(),
                    )
                }
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }
    }

    private fun TestApplicationContext.getGyldigToken(): String {
        val systembruker = Systembruker(
            roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_MELDEKORT)),
            klientnavn = "klientnavn",
            klientId = "id",
        )
        val token = this.jwtGenerator.createJwtForSystembruker(roles = listOf("les-meldekort"))
        texasClient.leggTilSystembruker(token, systembruker)
        return token
    }
}
