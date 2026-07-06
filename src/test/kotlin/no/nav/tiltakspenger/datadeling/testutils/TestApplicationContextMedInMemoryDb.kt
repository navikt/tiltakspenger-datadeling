package no.nav.tiltakspenger.datadeling.testutils

import no.nav.tiltakspenger.datadeling.fakes.ArenaFakeClient
import no.nav.tiltakspenger.datadeling.fakes.FakeBehandlingRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeGodkjentMeldekortbehandlingRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeMeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeSakRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeVedtakRepo
import no.nav.tiltakspenger.datadeling.infra.ApplicationContext
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory

class TestApplicationContextMedInMemoryDb(
    override val clock: TikkendeKlokke,
    override val texasClient: TexasClientFake,
    override val sessionFactory: TestSessionFactory = TestSessionFactory(),
) : ApplicationContext(
    clock = clock,
) {
    val jwtGenerator = JwtGenerator(clock = clock)
    override val behandlingRepo = FakeBehandlingRepo()
    override val vedtakRepo = FakeVedtakRepo()
    override val meldeperiodeRepo = FakeMeldeperiodeRepo()
    override val godkjentMeldekortbehandlingRepo = FakeGodkjentMeldekortbehandlingRepo()
    override val sakRepo = FakeSakRepo(
        vedtakRepo = vedtakRepo,
        behandlingRepo = behandlingRepo,
    )
    override val hentSakRepo = sakRepo
    override val arenaClient = ArenaFakeClient()
}
