package no.nav.tiltakspenger.datadeling.felles

import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.motta.infra.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.motta.infra.db.VedtakRepo
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import javax.sql.DataSource

internal class TestDataHelper(
    private val dataSource: DataSource,
) {
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val vedtakRepo = VedtakRepo(sessionFactory)
    val behandlingRepo = BehandlingRepo(sessionFactory)
}

private val dbManager = TestDatabaseManager()

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
internal fun withMigratedDb(runIsolated: Boolean = false, test: (TestDataHelper) -> Unit) {
    dbManager.withMigratedDb(runIsolated, test)
}
