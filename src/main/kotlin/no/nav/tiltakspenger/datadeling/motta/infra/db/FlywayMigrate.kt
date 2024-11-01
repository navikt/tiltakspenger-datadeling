package no.nav.tiltakspenger.datadeling.motta.infra.db

import no.nav.tiltakspenger.datadeling.Configuration
import no.nav.tiltakspenger.datadeling.Profile
import org.flywaydb.core.Flyway

private fun flyway(dataSource: javax.sql.DataSource): Flyway =
    when (Configuration.applicationProfile()) {
        Profile.LOCAL -> localFlyway(dataSource)
        Profile.DEV, Profile.PROD -> gcpFlyway(dataSource)
    }

private fun localFlyway(dataSource: javax.sql.DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .locations("db/migration", "db/local-migration")
        .dataSource(dataSource)
        .load()

private fun gcpFlyway(dataSource: javax.sql.DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .dataSource(dataSource)
        .load()

fun flywayMigrate(dataSource: javax.sql.DataSource) {
    flyway(dataSource).migrate()
}
