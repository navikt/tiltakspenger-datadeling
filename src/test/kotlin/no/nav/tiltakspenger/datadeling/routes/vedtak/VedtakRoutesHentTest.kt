package no.nav.tiltakspenger.datadeling.routes.vedtak

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.domene.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.jacksonSerialization
import no.nav.tiltakspenger.datadeling.routes.TestApplicationContext
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.datadeling.setupAuthentication
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.satser.Satser.Companion.sats
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakRoutesHentTest {
    private val satser2024 = sats(1.januar(2024))

    @Test
    fun `et vedtak med tiltakspenger`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            val periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31))
            coEvery { vedtakService.hentTpVedtak(any(), any()) } returns listOf(
                TiltakspengerVedtak(
                    periode = periode,
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "",
                    sakId = "",
                    saksnummer = "12345",
                    fnr = Fnr.random(),
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = Barnetillegg(perioder = listOf(BarnetilleggPeriode(antallBarn = 1, periode = Periode(periode.fraOgMed, periode.tilOgMed)))),
                ),
            )
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/detaljer")
                    },

                    jwt = token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2021-01-01",
                            "tom": "2021-12-31"
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
                                // language=JSON
                                """[
                            {
                              "fom":"2020-01-01",
                              "tom":"2024-12-31",
                              "rettighet":"TILTAKSPENGER",
                              "vedtakId": "",  
                              "sakId": "",  
                              "saksnummer":"12345",
                              "kilde":"tp",
                              "sats":${satser2024.sats},
                              "satsBarnetillegg":0
                            }
                            ]
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun stansvedtak() {
        val fnr = Fnr.random()
        val saksnummer = "12345"
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            val periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30))
            coEvery { vedtakService.hentTpVedtak(any(), any()) } returns listOf(
                TiltakspengerVedtak(
                    periode = periode,
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "",
                    sakId = "",
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2024-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
                TiltakspengerVedtak(
                    periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 31)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.INGENTING,
                    vedtakId = "",
                    sakId = "",
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2024-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/detaljer")
                    },

                    jwt = token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2024-01-01",
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
                                // language=JSON
                                """[
                            {
                              "fom":"2024-01-01",
                              "tom":"2024-06-30",
                              "rettighet":"TILTAKSPENGER",
                              "vedtakId": "",  
                              "sakId": "",  
                              "saksnummer":"12345",
                              "kilde":"tp",
                              "sats":${satser2024.sats},
                              "satsBarnetillegg":0
                            },
                            {
                              "fom":"2024-07-01",
                              "tom":"2024-12-31",
                              "rettighet":"INGENTING",
                              "vedtakId": "",  
                              "sakId": "",  
                              "saksnummer":"12345",
                              "kilde":"tp",
                              "sats":null,
                              "satsBarnetillegg":null
                            }
                            ]
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `test at vi kan hente uten å oppgi dato`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            coEvery { vedtakService.hentTpVedtak(any(), any()) } returns listOf(
                TiltakspengerVedtak(
                    periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "",
                    sakId = "",
                    saksnummer = "12345",
                    fnr = Fnr.random(),
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/detaljer")
                    },

                    jwt = token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910"
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
                                // language=JSON
                                """[
                            {
                              "fom":"2020-01-01",
                              "tom":"2024-12-31",
                              "rettighet":"TILTAKSPENGER",
                              "vedtakId":"",
                              "sakId":"",
                              "saksnummer":"12345",
                              "kilde":"tp",
                              "sats":${satser2024.sats},
                              "satsBarnetillegg":0
                            }
                            ]
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `test at uten ident gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/detaljer")
                    },
                    jwt = token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "",
                            "fom": "2021-01-01",
                            "tom": "2021-12-31"
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
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                            { "feilmelding" : "Ident  er ugyldig. Må bestå av 11 siffer" }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `test at fom som ikke kan parses som en gyldig dato gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/detaljer")
                    },
                    jwt = token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "01234567891",
                            "fom": "202X-01-01",
                            "tom": "2021-12-31"
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
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                            { "feilmelding" : "Ugyldig datoformat for fom-dato: 202X-01-01" }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `test at tom som ikke kan parses som en gyldig dato gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/detaljer")
                    },
                    jwt = token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "01234567891",
                            "fom": "2020-01-01",
                            "tom": "202X-12-31"
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
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                            { "feilmelding" : "Ugyldig datoformat for tom-dato: 202X-12-31" }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `test at fom og tom gir feilmelding når de ikke kommer i riktig rekkefølge`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/detaljer")
                    },
                    jwt = token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "01234567891",
                            "fom": "2021-01-01",
                            "tom": "2020-12-31"
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
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                            { "feilmelding" : "Fra-dato 2021-01-01 ikke være etter til-dato 2020-12-31" }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }
}
