package no.nav.tiltakspenger.datadeling.infra.db

import com.zaxxer.hikari.HikariDataSource

object DataSourceSetup {

    fun createDatasource(url: String): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = url
            initializationFailTimeout = 5000
            connectionTimeout = 2000
            minimumIdle = 5
            maximumPoolSize = 10
        }.also {
            flywayMigrate(it)
        }
    }
}
