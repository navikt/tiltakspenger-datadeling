package no.nav.tiltakspenger.datadeling.sak.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testutils.shouldBeCloseTo
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Dekker [SakPostgresRepo] sine fire metoder mot en migrert testdatabase –
 * inkludert upsert-grenen i `lagre` og null-grenene i `hentForId` og
 * `hentForFnr`.
 */
class SakPostgresRepoTest {

    @Test
    fun `lagre - ny sak - kan hentes ut igjen på id og fnr`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(
                fnr = fnr,
                opprettet = LocalDateTime.parse("2024-01-15T10:30:00"),
            )

            sakRepo.lagre(sak)

            val fraId = sakRepo.hentForId(sak.id)!!
            fraId.id shouldBe sak.id
            fraId.fnr shouldBe sak.fnr
            fraId.saksnummer shouldBe sak.saksnummer
            fraId.opprettet shouldBeCloseTo sak.opprettet

            val fraFnr = sakRepo.hentForFnr(fnr)!!
            fraFnr.id shouldBe sak.id
        }
    }

    @Test
    fun `lagre - eksisterende id - oppdaterer fnr, saksnummer og opprettet`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val opprinneligFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val sak = SakMother.sak(
                fnr = opprinneligFnr,
                opprettet = LocalDateTime.parse("2024-01-15T10:30:00"),
            )
            sakRepo.lagre(sak)

            val oppdatert = sak.copy(
                fnr = nyttFnr,
                saksnummer = Saksnummer("202402021002"),
                opprettet = LocalDateTime.parse("2024-02-01T11:00:00"),
            )
            sakRepo.lagre(oppdatert)

            val fraDb = sakRepo.hentForId(sak.id)!!
            fraDb.fnr shouldBe nyttFnr
            fraDb.saksnummer shouldBe Saksnummer("202402021002")
            fraDb.opprettet shouldBeCloseTo LocalDateTime.parse("2024-02-01T11:00:00")
            sakRepo.hentForFnr(opprinneligFnr) shouldBe null
        }
    }

    @Test
    fun `hentForId - ukjent id - gir null`() {
        withMigratedDb { testDataHelper ->
            testDataHelper.sakRepo.hentForId(SakId.random()) shouldBe null
        }
    }

    @Test
    fun `hentForFnr - ukjent fnr - gir null`() {
        withMigratedDb { testDataHelper ->
            testDataHelper.sakRepo.hentForFnr(Fnr.random()) shouldBe null
        }
    }

    @Test
    fun `oppdaterFnr - bytter fnr på matchende rad og lar de andre være`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val urørtFnr = Fnr.random()

            val sak = SakMother.sak(fnr = gammeltFnr)
            val urørtSak = SakMother.sak(
                id = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAA",
                saksnummer = "202401021001",
                fnr = urørtFnr,
            )
            sakRepo.lagre(sak)
            sakRepo.lagre(urørtSak)

            sakRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)

            sakRepo.hentForFnr(gammeltFnr) shouldBe null
            sakRepo.hentForFnr(nyttFnr)!!.id shouldBe sak.id
            sakRepo.hentForFnr(urørtFnr)!!.id shouldBe urørtSak.id
        }
    }
}
