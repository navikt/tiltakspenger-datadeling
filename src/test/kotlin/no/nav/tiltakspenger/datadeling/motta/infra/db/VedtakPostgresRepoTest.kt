package no.nav.tiltakspenger.datadeling.motta.infra.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.domene.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.felles.VedtakMother
import no.nav.tiltakspenger.datadeling.felles.withMigratedDb
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Test

class VedtakPostgresRepoTest {

    @Test
    fun `kan lagre og hente vedtak`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.vedtakPostgresRepo

            val vedtak = VedtakMother.tiltakspengerVedtak()
            repo.lagre(vedtak)

            testDataHelper.sessionFactory.withSession { session ->
                repo.hentForVedtakIdOgKilde(vedtak.vedtakId, vedtak.kilde, session) shouldBe vedtak
            }

            val enDagFørFraOgMed = vedtak.periode.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = vedtak.periode.tilOgMed.plusDays(1)

            // Feil kilde
            repo.hentForFnrOgPeriode(vedtak.fnr, vedtak.periode, "arena") shouldBe emptyList()
            // periode før vedtak
            repo.hentForFnrOgPeriode(vedtak.fnr, Periode(enDagFørFraOgMed, enDagFørFraOgMed), "tp") shouldBe emptyList()
            // periode første dag i vedtak
            repo.hentForFnrOgPeriode(
                vedtak.fnr,
                Periode(vedtak.periode.fraOgMed, vedtak.periode.fraOgMed),
                "tp",
            ) shouldBe listOf(vedtak)
            // periode siste dag i vedtak
            repo.hentForFnrOgPeriode(
                vedtak.fnr,
                Periode(vedtak.periode.tilOgMed, vedtak.periode.tilOgMed),
                "tp",
            ) shouldBe listOf(vedtak)
            // periode etter vedtak
            repo.hentForFnrOgPeriode(
                vedtak.fnr,
                Periode(enDagEtterTilOgMed, enDagEtterTilOgMed),
                "tp",
            ) shouldBe emptyList()
        }
    }

    @Test
    fun `kan lagre og hente vedtak med barnetillegg`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.vedtakPostgresRepo

            val vedtak = VedtakMother.tiltakspengerVedtak()
            val vedtakMedBarnetillegg = vedtak.copy(
                barnetillegg = Barnetillegg(perioder = listOf(BarnetilleggPeriode(antallBarn = 1, periode = vedtak.periode))),
                rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            )
            repo.lagre(vedtakMedBarnetillegg)

            testDataHelper.sessionFactory.withSession { session ->
                repo.hentForVedtakIdOgKilde(vedtakMedBarnetillegg.vedtakId, vedtakMedBarnetillegg.kilde, session) shouldBe vedtakMedBarnetillegg
            }
        }
    }
}
