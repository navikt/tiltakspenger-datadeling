package no.nav.tiltakspenger.datadeling.vedtak.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.datadeling.vedtak.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HentSakPostgresRepoTest {

    @Test
    fun `hentSakForVedtakSak - tom sak - returnerer sak med tomme lister`() {
        withMigratedDb { testDataHelper ->
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            testDataHelper.sakRepo.lagre(sak)

            val hentetSak = testDataHelper.hentSakRepo.hentSakForVedtakSak(fnr)!!

            hentetSak.id shouldBe sak.id
            hentetSak.rammevedtak shouldBe emptyList()
            hentetSak.behandlinger shouldBe emptyList()
        }
    }

    @Test
    fun `hentSakForVedtakSak - sak med vedtak og behandlinger - fyller domenet for vedtak sak endpointet`() {
        withMigratedDb { testDataHelper ->
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            testDataHelper.sakRepo.lagre(sak)
            val vedtak = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
            )
            val behandling = BehandlingMother.tiltakspengerBehandling(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
            )
            testDataHelper.vedtakRepo.lagre(vedtak)
            testDataHelper.behandlingRepo.lagre(behandling)

            val hentetSak = testDataHelper.hentSakRepo.hentSakForVedtakSak(fnr)!!

            hentetSak.id shouldBe sak.id
            hentetSak.rammevedtak shouldBe listOf(vedtak)
            hentetSak.behandlinger shouldBe listOf(behandling)
        }
    }

    @Test
    fun `hentSakForVedtakSak - vedtak med barnetillegg - leser barnetillegg tilbake fra db`() {
        withMigratedDb { testDataHelper ->
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            testDataHelper.sakRepo.lagre(sak)
            val virkningsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
            val vedtakMedBarnetillegg = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                virkningsperiode = virkningsperiode,
                rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
                barnetillegg = Barnetillegg(
                    perioder = listOf(
                        BarnetilleggPeriode(
                            antallBarn = 1,
                            periode = virkningsperiode,
                        ),
                    ),
                ),
            )
            testDataHelper.vedtakRepo.lagre(vedtakMedBarnetillegg)

            val hentetSak = testDataHelper.hentSakRepo.hentSakForVedtakSak(fnr)!!

            hentetSak.rammevedtak shouldBe listOf(vedtakMedBarnetillegg)
        }
    }
}
