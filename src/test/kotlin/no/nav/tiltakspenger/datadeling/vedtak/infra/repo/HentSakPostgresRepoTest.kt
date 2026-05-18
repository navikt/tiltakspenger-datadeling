package no.nav.tiltakspenger.datadeling.vedtak.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test

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
}
