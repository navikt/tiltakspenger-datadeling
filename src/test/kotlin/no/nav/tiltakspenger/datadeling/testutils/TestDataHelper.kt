package no.nav.tiltakspenger.datadeling.testutils

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.behandling.db.PostgresBehandlingRepo
import no.nav.tiltakspenger.datadeling.meldekort.db.PostgresGodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.meldekort.db.PostgresMeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.sak.db.PostgresSakRepo
import no.nav.tiltakspenger.datadeling.vedtak.db.PostgresVedtakRepo
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import javax.sql.DataSource

internal class TestDataHelper(
    private val dataSource: DataSource,
) {
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val vedtakRepo = PostgresVedtakRepo(sessionFactory)
    val behandlingRepo = PostgresBehandlingRepo(sessionFactory)
    val meldeperiodeRepo = PostgresMeldeperiodeRepo(sessionFactory)
    val godkjentMeldekortRepo = PostgresGodkjentMeldekortRepo(sessionFactory)
    val sakRepo = PostgresSakRepo(sessionFactory)
}

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
internal fun withMigratedDb(runIsolated: Boolean = true, test: (TestDataHelper) -> Unit) {
    testDatabaseManager.withMigratedDb(runIsolated = runIsolated) { dataSource ->
        test(TestDataHelper(dataSource))
    }
}
