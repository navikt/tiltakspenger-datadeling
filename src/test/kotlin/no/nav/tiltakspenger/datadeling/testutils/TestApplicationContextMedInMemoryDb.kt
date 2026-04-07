package no.nav.tiltakspenger.datadeling.testutils

import no.nav.tiltakspenger.datadeling.application.context.ApplicationContext
import no.nav.tiltakspenger.datadeling.fakes.ArenaFakeClient
import no.nav.tiltakspenger.datadeling.fakes.FakeBehandlingRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeGodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeMeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeSakRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeVedtakRepo
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke

class TestApplicationContextMedInMemoryDb(
    override val clock: TikkendeKlokke,
    override val texasClient: TexasClientFake,
    override val sessionFactory: TestSessionFactory = TestSessionFactory(),
) : ApplicationContext(
    clock = clock,
) {
    val jwtGenerator = JwtGenerator()
    override val behandlingRepo = FakeBehandlingRepo()
    override val vedtakRepo = FakeVedtakRepo()
    override val meldeperiodeRepo = FakeMeldeperiodeRepo()
    override val godkjentMeldekortRepo = FakeGodkjentMeldekortRepo()
    override val sakRepo = FakeSakRepo()
    override val arenaClient = ArenaFakeClient()
}
