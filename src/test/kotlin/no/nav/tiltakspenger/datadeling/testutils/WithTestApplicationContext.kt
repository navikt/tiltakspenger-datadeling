package no.nav.tiltakspenger.datadeling.testutils

import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.datadeling.application.ktorSetup
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai

fun withTestApplicationContextInMemory(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(fixedClockAt(1.mai(2025))),
    texasClient: TexasClientFake = TexasClientFake(clock),
    sessionFactory: TestSessionFactory = TestSessionFactory(),
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContextMedInMemoryDb) -> Unit,
) {
    with(
        TestApplicationContextMedInMemoryDb(
            clock = clock,
            texasClient = texasClient,
            sessionFactory = sessionFactory,
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
