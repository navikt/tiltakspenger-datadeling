package no.nav.tiltakspenger.datadeling.vedtak.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test

class VedtakRepoTest {

    @Test
    fun `kan lagre og hente vedtak`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val vedtak = VedtakMother.tiltakspengerVedtak(sakId = sak.id)
            vedtakRepo.lagre(vedtak)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakId(vedtak.vedtakId, session)?.vedtak shouldBe vedtak
            }

            val enDagFørFraOgMed = vedtak.virkningsperiode.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = vedtak.virkningsperiode.tilOgMed.plusDays(1)

            // periode før vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(enDagFørFraOgMed, enDagFørFraOgMed),
            ) shouldBe emptyList()
            // periode første dag i vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(vedtak.virkningsperiode.fraOgMed, vedtak.virkningsperiode.fraOgMed),
            ).map { it.vedtak } shouldBe listOf(vedtak)
            // periode siste dag i vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(vedtak.virkningsperiode.tilOgMed, vedtak.virkningsperiode.tilOgMed),
            ).map { it.vedtak } shouldBe listOf(vedtak)
            // periode etter vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(enDagEtterTilOgMed, enDagEtterTilOgMed),
            ) shouldBe emptyList()
        }
    }

    @Test
    fun `kan lagre og hente vedtak med barnetillegg`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val vedtak = VedtakMother.tiltakspengerVedtak(sakId = sak.id)
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
            vedtakRepo.lagre(vedtakMedBarnetillegg)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakId(
                    vedtakMedBarnetillegg.vedtakId,
                    session,
                )?.vedtak shouldBe vedtakMedBarnetillegg
            }
        }
    }

    @Test
    fun `kan lagre og hente avslagsvedtak`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val vedtak = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET),
            )
            vedtakRepo.lagre(vedtak)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakId(vedtak.vedtakId, session)?.vedtak shouldBe vedtak
            }
        }
    }
}
