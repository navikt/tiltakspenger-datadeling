package no.nav.tiltakspenger.datadeling.vedtak.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Test

class VedtakRepoTest {

    @Test
    fun `kan lagre og hente vedtak`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.vedtakRepo

            val vedtak = VedtakMother.tiltakspengerVedtak()
            repo.lagre(vedtak)

            testDataHelper.sessionFactory.withSession { session ->
                repo.hentForVedtakIdOgKilde(vedtak.vedtakId, vedtak.kilde, session) shouldBe vedtak
            }

            val enDagFørFraOgMed = vedtak.virkningsperiode.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = vedtak.virkningsperiode.tilOgMed.plusDays(1)

            // Feil kilde
            repo.hentForFnrOgPeriode(vedtak.fnr, vedtak.virkningsperiode, Kilde.ARENA) shouldBe emptyList()
            // periode før vedtak
            repo.hentForFnrOgPeriode(
                vedtak.fnr,
                Periode(enDagFørFraOgMed, enDagFørFraOgMed),
                Kilde.TPSAK,
            ) shouldBe emptyList()
            // periode første dag i vedtak
            repo.hentForFnrOgPeriode(
                vedtak.fnr,
                Periode(vedtak.virkningsperiode.fraOgMed, vedtak.virkningsperiode.fraOgMed),
                Kilde.TPSAK,
            ) shouldBe listOf(vedtak)
            // periode siste dag i vedtak
            repo.hentForFnrOgPeriode(
                vedtak.fnr,
                Periode(vedtak.virkningsperiode.tilOgMed, vedtak.virkningsperiode.tilOgMed),
                Kilde.TPSAK,
            ) shouldBe listOf(vedtak)
            // periode etter vedtak
            repo.hentForFnrOgPeriode(
                vedtak.fnr,
                Periode(enDagEtterTilOgMed, enDagEtterTilOgMed),
                Kilde.TPSAK,
            ) shouldBe emptyList()
        }
    }

    @Test
    fun `kan lagre og hente vedtak med barnetillegg`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.vedtakRepo

            val vedtak = VedtakMother.tiltakspengerVedtak()
            val vedtakMedBarnetillegg = vedtak.copy(
                barnetillegg = Barnetillegg(
                    perioder = listOf(
                        BarnetilleggPeriode(
                            antallBarn = 1,
                            periode = Periode(vedtak.virkningsperiode.fraOgMed, vedtak.virkningsperiode.tilOgMed),
                        ),
                    ),
                ),
                rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            )
            repo.lagre(vedtakMedBarnetillegg)

            testDataHelper.sessionFactory.withSession { session ->
                repo.hentForVedtakIdOgKilde(
                    vedtakMedBarnetillegg.vedtakId,
                    vedtakMedBarnetillegg.kilde,
                    session,
                ) shouldBe vedtakMedBarnetillegg
            }
        }
    }

    @Test
    fun `kan lagre og hente avslagsvedtak`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.vedtakRepo

            val vedtak = VedtakMother.tiltakspengerVedtak(
                rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET),
            )
            repo.lagre(vedtak)

            testDataHelper.sessionFactory.withSession { session ->
                repo.hentForVedtakIdOgKilde(vedtak.vedtakId, vedtak.kilde, session) shouldBe vedtak
            }
        }
    }
}
