package no.nav.tiltakspenger.datadeling.motta.infra.db

import com.zaxxer.hikari.HikariDataSource

object DataSourceSetup {

    fun createDatasource(url: String): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = url
            initializationFailTimeout = 5000
            maximumPoolSize = 5
        }.also {
            flywayMigrate(it)
        }
    }
}
