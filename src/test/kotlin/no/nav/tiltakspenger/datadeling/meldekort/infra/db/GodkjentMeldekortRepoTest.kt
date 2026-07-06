package no.nav.tiltakspenger.datadeling.meldekort.infra.db
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekort
import no.nav.tiltakspenger.datadeling.testdata.MeldekortMother
import no.nav.tiltakspenger.datadeling.testdata.MeldeperiodeMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testutils.shouldBeCloseTo
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GodkjentMeldekortRepoTest {
    val sakId = SakId.random()

    @Test
    fun `kan lagre og hente godkjent meldekort`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val sak = SakMother.sak(id = sakId.toString())
            sakRepo.lagre(sak)
            val meldeperiode = MeldeperiodeMother.meldeperiode(sakId = sakId)
            meldeperiodeRepo.lagre(listOf(meldeperiode))

            val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode)

            godkjentMeldekortRepo.lagre(godkjentMeldekort)

            val godkjenteMeldekortFraDb = godkjentMeldekortRepo.hentForFnrOgPeriode(
                sak.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )

            godkjenteMeldekortFraDb.size shouldBe 1
            val meldekortFraDb = godkjenteMeldekortFraDb.first()
            sammenlignGodkjentMeldekort(meldekortFraDb, godkjentMeldekort)
        }
    }

    @Test
    fun `oppdaterer eksisterende godkjent meldekort`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val sak = SakMother.sak(id = sakId.toString())
            sakRepo.lagre(sak)
            val meldeperiode = MeldeperiodeMother.meldeperiode(sakId = sakId)
            meldeperiodeRepo.lagre(listOf(meldeperiode))

            val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode)
            godkjentMeldekortRepo.lagre(godkjentMeldekort)

            val oppdatertGodkjentMeldekort = godkjentMeldekort.copy(
                mottattTidspunkt = null,
                vedtattTidspunkt = LocalDateTime.now(),
                behandletAutomatisk = false,
                meldeperioder = godkjentMeldekort.meldeperioder.map { meldeperiode ->
                    meldeperiode.copy(
                        korrigert = true,
                        meldekortdager = meldeperiode.meldekortdager.map { dag ->
                            if (dag.dato.isBefore(meldeperiode.fraOgMed.plusDays(4)) &&
                                dag.status == GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET
                            ) {
                                dag.copy(
                                    status = GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.FRAVAER_SYK,
                                    reduksjon = GodkjentMeldekort.MeldekortDag.Reduksjon.UKJENT,
                                )
                            } else {
                                dag
                            }
                        },
                    )
                },
            )
            godkjentMeldekortRepo.lagre(oppdatertGodkjentMeldekort)

            val godkjenteMeldekortFraDb = godkjentMeldekortRepo.hentForFnrOgPeriode(
                sak.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )

            godkjenteMeldekortFraDb.size shouldBe 1
            val meldekortFraDb = godkjenteMeldekortFraDb.first()
            sammenlignGodkjentMeldekort(meldekortFraDb, oppdatertGodkjentMeldekort)
        }
    }

    @Test
    fun `kan lagre godkjent meldekort selv om tilhørende meldeperiode ikke er lagret`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val sak = SakMother.sak(id = sakId.toString())
            sakRepo.lagre(sak)
            val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
            val meldeperiode = MeldeperiodeMother.meldeperiode(sakId = sakId)

            val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode)

            godkjentMeldekortRepo.lagre(godkjentMeldekort)

            val godkjenteMeldekortFraDb = godkjentMeldekortRepo.hentForFnrOgPeriode(
                sak.fnr,
                Periode(
                    fraOgMed = meldeperiode.fraOgMed.minusDays(5),
                    tilOgMed = meldeperiode.tilOgMed.plusDays(5),
                ),
            )

            godkjenteMeldekortFraDb.size shouldBe 1
            sammenlignGodkjentMeldekort(godkjenteMeldekortFraDb.first(), godkjentMeldekort)
        }
    }
}

fun sammenlignGodkjentMeldekort(actual: GodkjentMeldekort, expected: GodkjentMeldekort) {
    actual.meldekortbehandlingId shouldBe expected.meldekortbehandlingId
    actual.sakId shouldBe expected.sakId
    actual.meldeperioder shouldBe expected.meldeperioder
    actual.mottattTidspunkt shouldBeCloseTo expected.mottattTidspunkt
    actual.vedtattTidspunkt shouldBeCloseTo expected.vedtattTidspunkt
    actual.behandletAutomatisk shouldBe expected.behandletAutomatisk
    actual.fraOgMed shouldBe expected.fraOgMed
    actual.tilOgMed shouldBe expected.tilOgMed
    actual.journalpostId shouldBe expected.journalpostId
    actual.totaltBelop shouldBe expected.totaltBelop
    actual.totalDifferanse shouldBe expected.totalDifferanse
    actual.barnetillegg shouldBe expected.barnetillegg
    actual.opprettet shouldBeCloseTo expected.opprettet
    actual.sistEndret shouldBeCloseTo expected.sistEndret
}
