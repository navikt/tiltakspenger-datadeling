package no.nav.tiltakspenger.datadeling.application.db

import com.zaxxer.hikari.HikariDataSource

object DataSourceSetup {

    fun createDatasource(url: String): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = url
            initializationFailTimeout = 5000
            maximumPoolSize = 10
        }.also {
            flywayMigrate(it)
        }
    }
}
