package no.nav.tiltakspenger.datadeling.sak.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test

class SakRepoTest {

    @Test
    fun `hentForFnr returnerer null for tom sak uten behandling eller rammevedtak`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)

            sakRepo.hentForFnr(fnr) shouldBe null
        }
    }

    @Test
    fun `hentForFnr returnerer sak med behandling`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val behandlingRepo = testDataHelper.behandlingRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            val behandling = BehandlingMother.tiltakspengerBehandling(sakId = sak.id)
            sakRepo.lagre(sak)
            behandlingRepo.lagre(behandling)

            val hentetSak = sakRepo.hentForFnr(fnr)!!

            hentetSak.id shouldBe sak.id
            hentetSak.behandlinger shouldBe listOf(behandling)
            hentetSak.rammevedtak shouldBe emptyList()
        }
    }

    @Test
    fun `hentForFnr returnerer sak med rammevedtak`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            val vedtak = VedtakMother.tiltakspengerVedtak(sakId = sak.id)
            sakRepo.lagre(sak)
            vedtakRepo.lagre(vedtak)

            val hentetSak = sakRepo.hentForFnr(fnr)!!

            hentetSak.id shouldBe sak.id
            hentetSak.behandlinger shouldBe emptyList()
            hentetSak.rammevedtak shouldBe listOf(vedtak)
        }
    }
}
