package no.nav.tiltakspenger.datadeling.testutils

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.libs.persistering.test.common.TestDatabaseConfig
import javax.sql.DataSource
import no.nav.tiltakspenger.libs.persistering.test.common.TestDatabaseManager as LibsTestDatabaseManager

class TestDatabaseManager(
    config: TestDatabaseConfig = TestDatabaseConfig(),
) {
    private val delegate = LibsTestDatabaseManager(
        config = config,
        idGeneratorsFactory = { },
    )

    fun withMigratedDb(
        runIsolated: Boolean = false,
        test: (DataSource) -> Unit,
    ) {
        delegate.withMigratedDb(runIsolated = runIsolated) { _, _, _ ->
            test(delegate.dataSource(runIsolated))
        }
    }

    /**
     * Oppretter et tomt, umigrert skjema i den delte test-containeren og gir url-en til det.
     *
     * `persistering-test-common` deler bare ut ferdig migrerte datakilder, slik at tester ikke kan tråkke på hverandres skjemaer.
     * Tester som skal verifisere vår egen oppkobling (Hikari + Flyway) trenger likevel en rå url — uten dette måtte de startet en container til, som er det dyreste enkeltsteget i testkjøringen.
     * Skjemaet er skilt fra `parallel` og `isolated`, så de andre testene er upåvirket.
     */
    fun urlTilTomtSkjema(skjema: String): String {
        val delt = delegate.dataSource() as? HikariDataSource
            ?: error("Forventet at persistering-test-common gir en HikariDataSource å hente jdbc-url fra")

        sessionOf(delt).use { session ->
            session.run(queryOf("create schema if not exists $skjema").asExecute)
        }

        val utenSkjema = delt.jdbcUrl.substringBefore("&currentSchema=")
        return "$utenSkjema&currentSchema=$skjema&user=${delt.username}&password=${delt.password}"
    }
}

val testDatabaseManager = TestDatabaseManager()
