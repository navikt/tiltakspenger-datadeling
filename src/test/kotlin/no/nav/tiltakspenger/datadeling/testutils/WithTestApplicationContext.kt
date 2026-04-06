package no.nav.tiltakspenger.datadeling.testutils
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.application.configureExceptions
import no.nav.tiltakspenger.datadeling.application.jacksonSerialization
import no.nav.tiltakspenger.datadeling.application.setupAuthentication
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes.meldekortRoutes
import no.nav.tiltakspenger.datadeling.routes.healthRoutes
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.texas.IdentityProvider
fun withTestApplicationContext(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(fixedClockAt(1.mai(2025))),
    texasClient: TexasClientFake = TexasClientFake(clock),
    sessionFactory: TestSessionFactory = TestSessionFactory(),
    tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContextMedInMemoryDb) -> Unit,
) {
    with(
        TestApplicationContextMedInMemoryDb(
            clock = clock,
            texasClient = texasClient,
            sessionFactory = sessionFactory,
            tilgangsmaskinFakeClient = tilgangsmaskinFakeClient,
        ),
    ) {
        val tac = this
        testApplication {
            application {
                ktorSetup(tac)
                additionalConfig()
            }
            testBlock(this@with)
        }
    }
}
private fun Application.ktorSetup(tac: TestApplicationContextMedInMemoryDb) {
    jacksonSerialization()
    configureExceptions()
    setupAuthentication(tac.texasClient)
    routing {
        healthRoutes()
        authenticate(IdentityProvider.AZUREAD.value) {
            vedtakRoutes(mockk(relaxed = true))
            meldekortRoutes(mockk(relaxed = true))
        }
    }
}
