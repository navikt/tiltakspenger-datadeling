package no.nav.tiltakspenger.datadeling.identhendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test
import java.util.UUID

class IdenthendelseServiceTest {
    @Test
    fun `behandleIdenthendelse - finnes vedtak og behandling på gammelt fnr - oppdaterer`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val identhendelseService = IdenthendelseService(behandlingRepo, vedtakRepo)

            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val behandling = BehandlingMother.tiltakspengerBehandling(fnr = gammeltFnr)
            behandlingRepo.lagre(behandling)
            val vedtak = VedtakMother.tiltakspengerVedtak(fnr = gammeltFnr)
            vedtakRepo.lagre(vedtak)
            val urelatertFnr = Fnr.random()
            behandlingRepo.lagre(BehandlingMother.tiltakspengerBehandling(fnr = urelatertFnr))
            vedtakRepo.lagre(VedtakMother.tiltakspengerVedtak(fnr = urelatertFnr))

            identhendelseService.behandleIdenthendelse(
                id = UUID.randomUUID(),
                identhendelseDto = IdenthendelseDto(gammeltFnr = gammeltFnr.verdi, nyttFnr = nyttFnr.verdi),
            )

            behandlingRepo.hentForFnr(gammeltFnr) shouldBe null
            behandlingRepo.hentForFnr(nyttFnr) shouldBe behandling.copy(fnr = nyttFnr)
            vedtakRepo.hentForFnr(gammeltFnr).firstOrNull() shouldBe null
            vedtakRepo.hentForFnr(nyttFnr).firstOrNull() shouldBe vedtak.copy(fnr = nyttFnr)

            behandlingRepo.hentForFnr(urelatertFnr) shouldNotBe null
            vedtakRepo.hentForFnr(urelatertFnr).firstOrNull() shouldNotBe null
        }
    }
}
