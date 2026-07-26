package no.nav.tiltakspenger.datadeling.infra.db

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.datadeling.infra.Profile
import no.nav.tiltakspenger.datadeling.testutils.testDatabaseManager
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Oppkoblingen mot databasen ved oppstart.
 *
 * [DataSourceSetup.createDatasource] kjører Flyway som en del av oppkoblingen, slik at appen feiler ved oppstart hvis migreringene ikke går gjennom, framfor å starte med et halvferdig skjema.
 *
 * Hver profil får sitt eget skjema i den delte test-containeren — profilene kjører ulike sett med migreringer, så de kan ikke dele skjema.
 */
class DataSourceSetupTest {

    @ParameterizedTest(name = "profil {0}")
    @EnumSource(Profile::class)
    fun `kobler opp og kjorer migreringene for hver profil`(profile: Profile) {
        val skjema = "oppkobling_${profile.name.lowercase()}"

        val dataSource = DataSourceSetup.createDatasource(
            url = testDatabaseManager.urlTilTomtSkjema(skjema),
            profile = profile,
        )

        sessionOf(dataSource).use { session ->
            val antallMigreringer = session.run(
                queryOf("select count(*) as antall from flyway_schema_history where success = true")
                    .map { it.int("antall") }
                    .asSingle,
            )
            antallMigreringer!! shouldBeGreaterThan 0

            val finnesRammevedtak = session.run(
                queryOf("select to_regclass('$skjema.rammevedtak') is not null as finnes")
                    .map { it.boolean("finnes") }
                    .asSingle,
            )
            finnesRammevedtak shouldBe true
        }

        dataSource.close()
    }
}
