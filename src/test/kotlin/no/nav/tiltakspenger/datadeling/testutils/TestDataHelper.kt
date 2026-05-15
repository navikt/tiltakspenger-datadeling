package no.nav.tiltakspenger.datadeling.testutils

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.behandling.infra.BehandlingPostgresRepo
import no.nav.tiltakspenger.datadeling.meldekort.infra.GodkjentMeldekortPostgresRepo
import no.nav.tiltakspenger.datadeling.meldekort.infra.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.datadeling.sak.infra.SakPostgresRepo
import no.nav.tiltakspenger.datadeling.vedtak.infra.VedtakPostgresRepo
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import javax.sql.DataSource

internal class TestDataHelper(
    private val dataSource: DataSource,
) {
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val vedtakRepo = VedtakPostgresRepo(sessionFactory)
    val behandlingRepo = BehandlingPostgresRepo(sessionFactory)
    val meldeperiodeRepo = MeldeperiodePostgresRepo(sessionFactory)
    val godkjentMeldekortRepo = GodkjentMeldekortPostgresRepo(sessionFactory)
    val sakRepo = SakPostgresRepo(sessionFactory)
}

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
internal fun withMigratedDb(runIsolated: Boolean = true, test: (TestDataHelper) -> Unit) {
    testDatabaseManager.withMigratedDb(runIsolated = runIsolated) { dataSource ->
        test(TestDataHelper(dataSource))
    }
}
