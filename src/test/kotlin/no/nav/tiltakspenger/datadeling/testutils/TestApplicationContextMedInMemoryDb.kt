package no.nav.tiltakspenger.datadeling.testutils
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
class TestApplicationContextMedInMemoryDb(
    val clock: TikkendeKlokke,
    val texasClient: TexasClientFake,
    val sessionFactory: TestSessionFactory,
    val tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient,
) {
    val jwtGenerator = JwtGenerator()
}
