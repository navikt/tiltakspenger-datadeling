package no.nav.tiltakspenger.datadeling.meldekort.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.meldekort.domene.GodkjentMeldekort
import no.nav.tiltakspenger.datadeling.testdata.MeldekortMother
import no.nav.tiltakspenger.datadeling.testdata.MeldeperiodeMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testutils.shouldBeCloseTo
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLException
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
                korrigert = true,
                meldekortdager = meldeperiode.girRett.map {
                    if (it.value && it.key.isBefore(meldeperiode.fraOgMed.plusDays(4))) {
                        GodkjentMeldekort.MeldekortDag(
                            dato = it.key,
                            status = GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.FRAVAER_SYK,
                            reduksjon = GodkjentMeldekort.MeldekortDag.Reduksjon.UKJENT,
                        )
                    } else if (it.value) {
                        GodkjentMeldekort.MeldekortDag(
                            dato = it.key,
                            status = GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET,
                            reduksjon = GodkjentMeldekort.MeldekortDag.Reduksjon.INGEN_REDUKSJON,
                        )
                    } else {
                        GodkjentMeldekort.MeldekortDag(
                            dato = it.key,
                            status = GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.IKKE_TILTAKSDAG,
                            reduksjon = GodkjentMeldekort.MeldekortDag.Reduksjon.YTELSEN_FALLER_BORT,
                        )
                    }
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
    fun `kan ikke lagre godkjent meldekort hvis tilhørende meldeperiode ikke finnes`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val sak = SakMother.sak(id = sakId.toString())
            sakRepo.lagre(sak)
            val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
            val meldeperiode = MeldeperiodeMother.meldeperiode(sakId = sakId)

            val godkjentMeldekort = MeldekortMother.godkjentMeldekort(meldeperiode)

            assertThrows<PSQLException> {
                godkjentMeldekortRepo.lagre(godkjentMeldekort)
            }
        }
    }
}

fun sammenlignGodkjentMeldekort(actual: GodkjentMeldekort, expected: GodkjentMeldekort) {
    actual.meldekortbehandlingId shouldBe expected.meldekortbehandlingId
    actual.kjedeId shouldBe expected.kjedeId
    actual.sakId shouldBe expected.sakId
    actual.meldeperiodeId shouldBe expected.meldeperiodeId
    actual.mottattTidspunkt shouldBeCloseTo expected.mottattTidspunkt
    actual.vedtattTidspunkt shouldBeCloseTo expected.vedtattTidspunkt
    actual.behandletAutomatisk shouldBe expected.behandletAutomatisk
    actual.korrigert shouldBe expected.korrigert
    actual.fraOgMed shouldBe expected.fraOgMed
    actual.tilOgMed shouldBe expected.tilOgMed
    actual.meldekortdager shouldBe expected.meldekortdager
    actual.journalpostId shouldBe expected.journalpostId
    actual.totaltBelop shouldBe expected.totaltBelop
    actual.totalDifferanse shouldBe expected.totalDifferanse
    actual.barnetillegg shouldBe expected.barnetillegg
    actual.opprettet shouldBeCloseTo expected.opprettet
    actual.sistEndret shouldBeCloseTo expected.sistEndret
}
