package no.nav.tiltakspenger.datadeling.application.db

import no.nav.tiltakspenger.datadeling.Configuration
import no.nav.tiltakspenger.datadeling.Profile
import org.flywaydb.core.Flyway
import javax.sql.DataSource

private fun flyway(dataSource: DataSource): Flyway =
    when (Configuration.applicationProfile()) {
        Profile.LOCAL -> localFlyway(dataSource)
        Profile.DEV, Profile.PROD -> gcpFlyway(dataSource)
    }

private fun localFlyway(dataSource: DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .locations("db/migration", "db/local-migration")
        .dataSource(dataSource)
        .load()

private fun gcpFlyway(dataSource: DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .dataSource(dataSource)
        .load()

fun flywayMigrate(dataSource: DataSource) {
    flyway(dataSource).migrate()
}
