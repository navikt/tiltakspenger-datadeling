package no.nav.tiltakspenger.datadeling.infra.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.tiltakspenger.datadeling.infra.Profile

object DataSourceSetup {

    /**
     * Kobler opp mot databasen og kjører migreringene som en del av oppkoblingen, slik at appen feiler ved oppstart framfor å starte med et halvferdig skjema.
     */
    fun createDatasource(url: String, profile: Profile): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = url
            initializationFailTimeout = 5000
            connectionTimeout = 2000
            minimumIdle = 5
            maximumPoolSize = 10
        }.also {
            flywayMigrate(it, profile)
        }
    }
}
