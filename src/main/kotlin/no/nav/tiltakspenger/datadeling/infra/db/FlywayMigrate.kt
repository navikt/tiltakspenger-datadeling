package no.nav.tiltakspenger.datadeling.infra.db

import no.nav.tiltakspenger.datadeling.infra.Profile
import org.flywaydb.core.Flyway
import javax.sql.DataSource

/**
 * Lokalt kjører vi i tillegg migreringene under `db/local-migration`, som seeder testdata.
 * I dev og prod kjører kun `db/migration`.
 *
 * Profilen sendes inn framfor å leses fra `Configuration` her, slik at begge variantene kan settes opp fra test uten å mutere global system-env.
 */
private fun flyway(dataSource: DataSource, profile: Profile): Flyway =
    when (profile) {
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

fun flywayMigrate(dataSource: DataSource, profile: Profile) {
    flyway(dataSource, profile).migrate()
}
