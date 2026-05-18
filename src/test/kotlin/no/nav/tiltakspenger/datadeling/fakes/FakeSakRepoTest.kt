package no.nav.tiltakspenger.datadeling.fakes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import org.junit.jupiter.api.Test

class FakeSakRepoTest {

    @Test
    fun `hentSakForVedtakSak henter vedtak og behandlinger fra relevante fakes`() {
        val vedtakRepo = FakeVedtakRepo()
        val behandlingRepo = FakeBehandlingRepo()
        val sakRepo = FakeSakRepo(
            vedtakRepo = vedtakRepo,
            behandlingRepo = behandlingRepo,
        )
        val sak = SakMother.sak()
        val annenSak = SakMother.sak(
            id = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAA",
            saksnummer = "202401021001",
        )
        val vedtak = VedtakMother.tiltakspengerVedtak(
            sakId = sak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
        )
        val annenSakVedtak = VedtakMother.tiltakspengerVedtak(
            sakId = annenSak.id,
            fnr = annenSak.fnr,
            saksnummer = annenSak.saksnummer,
        )
        val behandling = BehandlingMother.tiltakspengerBehandling(
            sakId = sak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
        )
        val annenSakBehandling = BehandlingMother.tiltakspengerBehandling(
            sakId = annenSak.id,
            fnr = annenSak.fnr,
            saksnummer = annenSak.saksnummer,
        )
        sakRepo.lagre(sak)
        sakRepo.lagre(annenSak)
        vedtakRepo.lagre(vedtak)
        vedtakRepo.lagre(annenSakVedtak)
        behandlingRepo.lagre(behandling)
        behandlingRepo.lagre(annenSakBehandling)

        val hentetSak = sakRepo.hentSakForVedtakSak(sak.fnr)!!

        hentetSak.id shouldBe sak.id
        hentetSak.rammevedtak shouldBe listOf(vedtak)
        hentetSak.behandlinger shouldBe listOf(behandling)
    }
}
