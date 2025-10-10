package no.nav.tiltakspenger.datadeling.meldekort.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.datadeling.testdata.MeldekortMother
import no.nav.tiltakspenger.datadeling.testdata.MeldeperiodeMother
import no.nav.tiltakspenger.datadeling.testutils.shouldBeCloseTo
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MeldeperiodeRepoTest {
    @Test
    fun `kan lagre og hente meldeperiode`() {
        withMigratedDb { testDataHelper ->
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldeperiode = MeldeperiodeMother.meldeperiode()

            meldeperiodeRepo.lagre(listOf(meldeperiode))

            val meldeperioderFraDb = meldeperiodeRepo.hentForFnrOgPeriode(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )

            meldeperioderFraDb.size shouldBe 1
            val meldeperiodeFraDb = meldeperioderFraDb.first()
            sammenlignMeldeperiode(meldeperiodeFraDb, meldeperiode)
        }
    }

    @Test
    fun `lagrer ikke meldeperiode hvis ingen dager gir rett`() {
        withMigratedDb { testDataHelper ->
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val periode = MeldeperiodeMother.periode()
            val meldeperiode = MeldeperiodeMother.meldeperiode(
                periode = periode,
                girRett = periode.tilDager().associateWith { false },
            )

            meldeperiodeRepo.lagre(listOf(meldeperiode))

            val meldeperioderFraDb = meldeperiodeRepo.hentForFnrOgPeriode(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )

            meldeperioderFraDb.size shouldBe 0
        }
    }

    @Test
    fun `oppdaterer eksisterende meldeperioder`() {
        withMigratedDb { testDataHelper ->
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldeperiode = MeldeperiodeMother.meldeperiode()

            meldeperiodeRepo.lagre(listOf(meldeperiode))

            val girRett = Periode(
                fraOgMed = meldeperiode.fraOgMed,
                tilOgMed = meldeperiode.tilOgMed,
            ).tilDager()
                .associateWith { value ->
                    listOf(value.dayOfWeek).none { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } && value.isBefore(
                        meldeperiode.tilOgMed.minusDays(4),
                    )
                }
            val oppdatertMeldeperiode = meldeperiode.copy(
                id = MeldeperiodeId.random(),
                girRett = girRett,
                maksAntallDagerForPeriode = girRett.filter { it.value }.size,
            )

            meldeperiodeRepo.lagre(listOf(oppdatertMeldeperiode))

            val meldeperioderFraDb = meldeperiodeRepo.hentForFnrOgPeriode(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )

            meldeperioderFraDb.size shouldBe 1
            val meldeperiodeFraDb = meldeperioderFraDb.first()
            sammenlignMeldeperiode(meldeperiodeFraDb, oppdatertMeldeperiode)
        }
    }

    @Test
    fun `sletter eksisterende meldeperiode hvis ingen dager gir rett`() {
        withMigratedDb { testDataHelper ->
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val forstePeriode = MeldeperiodeMother.periode(
                fraSisteMandagFor = LocalDate.now().minusDays(14),
                tilSisteSondagEtter = null,
            )
            val meldeperiode1 = MeldeperiodeMother.meldeperiode(periode = forstePeriode)
            val andrePeriode =
                MeldeperiodeMother.periode(fraSisteMandagFor = LocalDate.now(), tilSisteSondagEtter = null)
            val meldeperiode2 = MeldeperiodeMother.meldeperiode(
                periode = andrePeriode,
                sakId = meldeperiode1.sakId,
            )

            meldeperiodeRepo.lagre(listOf(meldeperiode1, meldeperiode2))

            val oppdatertMeldeperiode = meldeperiode2.copy(
                id = MeldeperiodeId.random(),
                girRett = Periode(
                    fraOgMed = meldeperiode2.fraOgMed,
                    tilOgMed = meldeperiode2.tilOgMed,
                ).tilDager().associateWith { false },
                maksAntallDagerForPeriode = 0,
            )

            meldeperiodeRepo.lagre(listOf(meldeperiode1, oppdatertMeldeperiode))

            val meldeperioderFraDb = meldeperiodeRepo.hentForFnrOgPeriode(
                meldeperiode1.fnr,
                Periode(
                    fraOgMed = meldeperiode1.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode2.tilOgMed.plusDays(5),
                ),
            )

            meldeperioderFraDb.size shouldBe 1
            val meldeperiodeFraDb = meldeperioderFraDb.first()
            sammenlignMeldeperiode(meldeperiodeFraDb, meldeperiode1)
        }
    }

    @Test
    fun `oppdatere meldeperiode, finnes godkjent meldekort - sletter meldekort og oppdaterer meldeperiode`() {
        withMigratedDb { testDataHelper ->
            val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldeperiode = MeldeperiodeMother.meldeperiode()
            meldeperiodeRepo.lagre(listOf(meldeperiode))

            val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode)
            godkjentMeldekortRepo.lagre(godkjentMeldekort)

            val girRett = Periode(
                fraOgMed = meldeperiode.fraOgMed,
                tilOgMed = meldeperiode.tilOgMed,
            ).tilDager()
                .associateWith { value ->
                    listOf(value.dayOfWeek).none { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } && value.isBefore(
                        meldeperiode.tilOgMed.minusDays(4),
                    )
                }
            val oppdatertMeldeperiode = meldeperiode.copy(
                id = MeldeperiodeId.random(),
                girRett = girRett,
                maksAntallDagerForPeriode = girRett.filter { it.value }.size,
            )
            meldeperiodeRepo.lagre(listOf(oppdatertMeldeperiode))

            val meldeperioderFraDb = meldeperiodeRepo.hentForFnrOgPeriode(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )
            meldeperioderFraDb.size shouldBe 1
            val meldeperiodeFraDb = meldeperioderFraDb.first()
            sammenlignMeldeperiode(meldeperiodeFraDb, oppdatertMeldeperiode)

            val godkjenteMeldekortFraDb = godkjentMeldekortRepo.hentForFnrOgPeriode(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )
            godkjenteMeldekortFraDb.size shouldBe 0
        }
    }

    @Test
    fun `oppdatere meldeperiode, ingen dager gir rett, finnes godkjent meldekort - sletter meldekort og meldeperiode`() {
        withMigratedDb { testDataHelper ->
            val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldeperiode = MeldeperiodeMother.meldeperiode()
            meldeperiodeRepo.lagre(listOf(meldeperiode))

            val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode)
            godkjentMeldekortRepo.lagre(godkjentMeldekort)

            val girRett = Periode(meldeperiode.fraOgMed, meldeperiode.tilOgMed).tilDager().associateWith { false }
            val oppdatertMeldeperiode = meldeperiode.copy(
                id = MeldeperiodeId.random(),
                girRett = girRett,
                maksAntallDagerForPeriode = girRett.filter { it.value }.size,
            )
            meldeperiodeRepo.lagre(listOf(oppdatertMeldeperiode))

            val meldeperioderFraDb = meldeperiodeRepo.hentForFnrOgPeriode(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )
            meldeperioderFraDb.size shouldBe 0

            val godkjenteMeldekortFraDb = godkjentMeldekortRepo.hentForFnrOgPeriode(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )
            godkjenteMeldekortFraDb.size shouldBe 0
        }
    }

    @Test
    fun `hentMeldeperioderOgGodkjenteMeldekort - returnerer meldeperioder og meldekort`() {
        withMigratedDb { testDataHelper ->
            val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldeperiode = MeldeperiodeMother.meldeperiode()
            meldeperiodeRepo.lagre(listOf(meldeperiode))
            val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode)
            godkjentMeldekortRepo.lagre(godkjentMeldekort)

            val meldeperioderOgGodkjenteMeldekort = meldeperiodeRepo.hentMeldeperioderOgGodkjenteMeldekort(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )

            meldeperioderOgGodkjenteMeldekort.size shouldBe 1
            sammenlignMeldeperiode(meldeperioderOgGodkjenteMeldekort.first().meldeperiode, meldeperiode)
            meldeperioderOgGodkjenteMeldekort.first().godkjentMeldekort?.meldeperiodeId shouldBe godkjentMeldekort.meldeperiodeId
        }
    }

    @Test
    fun `hentMeldeperioderOgGodkjenteMeldekort - ingen meldekort - returnerer kun meldeperioder`() {
        withMigratedDb { testDataHelper ->
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldeperiode = MeldeperiodeMother.meldeperiode()
            meldeperiodeRepo.lagre(listOf(meldeperiode))

            val meldeperioderOgGodkjenteMeldekort = meldeperiodeRepo.hentMeldeperioderOgGodkjenteMeldekort(
                meldeperiode.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )

            meldeperioderOgGodkjenteMeldekort.size shouldBe 1
            sammenlignMeldeperiode(meldeperioderOgGodkjenteMeldekort.first().meldeperiode, meldeperiode)
            meldeperioderOgGodkjenteMeldekort.first().godkjentMeldekort shouldBe null
        }
    }

    private fun sammenlignMeldeperiode(actual: Meldeperiode, expected: Meldeperiode) {
        actual.id shouldBe expected.id
        actual.kjedeId shouldBe expected.kjedeId
        actual.fnr shouldBe expected.fnr
        actual.sakId shouldBe expected.sakId
        actual.saksnummer shouldBe expected.saksnummer
        actual.opprettet shouldBeCloseTo expected.opprettet
        actual.fraOgMed shouldBe expected.fraOgMed
        actual.tilOgMed shouldBe expected.tilOgMed
        actual.maksAntallDagerForPeriode shouldBe expected.maksAntallDagerForPeriode
        actual.girRett shouldBe expected.girRett
    }
}
