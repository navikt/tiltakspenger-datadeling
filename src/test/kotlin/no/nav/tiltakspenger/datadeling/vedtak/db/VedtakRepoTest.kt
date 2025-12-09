package no.nav.tiltakspenger.datadeling.vedtak.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
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
            vedtakRepo.lagre(vedtak, fnr, sak.saksnummer)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakIdOgKilde(vedtak.vedtakId, vedtak.kilde, session)?.vedtak shouldBe vedtak
            }

            val enDagFørFraOgMed = vedtak.virkningsperiode.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = vedtak.virkningsperiode.tilOgMed.plusDays(1)

            // Feil kilde
            vedtakRepo.hentForFnrOgPeriode(fnr, vedtak.virkningsperiode, Kilde.ARENA) shouldBe emptyList()
            // periode før vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(enDagFørFraOgMed, enDagFørFraOgMed),
                Kilde.TPSAK,
            ) shouldBe emptyList()
            // periode første dag i vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(vedtak.virkningsperiode.fraOgMed, vedtak.virkningsperiode.fraOgMed),
                Kilde.TPSAK,
            ).map { it.vedtak } shouldBe listOf(vedtak)
            // periode siste dag i vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(vedtak.virkningsperiode.tilOgMed, vedtak.virkningsperiode.tilOgMed),
                Kilde.TPSAK,
            ).map { it.vedtak } shouldBe listOf(vedtak)
            // periode etter vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(enDagEtterTilOgMed, enDagEtterTilOgMed),
                Kilde.TPSAK,
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
            vedtakRepo.lagre(vedtakMedBarnetillegg, fnr, sak.saksnummer)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakIdOgKilde(
                    vedtakMedBarnetillegg.vedtakId,
                    vedtakMedBarnetillegg.kilde,
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
            vedtakRepo.lagre(vedtak, fnr, sak.saksnummer)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakIdOgKilde(vedtak.vedtakId, vedtak.kilde, session)?.vedtak shouldBe vedtak
            }
        }
    }
}
