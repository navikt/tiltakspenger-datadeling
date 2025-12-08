package no.nav.tiltakspenger.datadeling.identhendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
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
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val godkjentMeldekortRepo = testDataHelper.godkjentMeldekortRepo
            val sakRepo = testDataHelper.sakRepo
            val identhendelseService = IdenthendelseService(behandlingRepo, vedtakRepo, meldeperiodeRepo, godkjentMeldekortRepo, sakRepo)

            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val sak = SakMother.sak(fnr = gammeltFnr)
            sakRepo.lagre(sak)
            val behandling = BehandlingMother.tiltakspengerBehandling(fnr = gammeltFnr)
            behandlingRepo.lagre(behandling)
            val vedtak = VedtakMother.tiltakspengerVedtak(fnr = gammeltFnr)
            vedtakRepo.lagre(vedtak)
            val urelatertFnr = Fnr.random()
            sakRepo.lagre(SakMother.sak(id = "id2", saksnummer = "saksnummer2", fnr = urelatertFnr))
            behandlingRepo.lagre(BehandlingMother.tiltakspengerBehandling(fnr = urelatertFnr))
            vedtakRepo.lagre(VedtakMother.tiltakspengerVedtak(fnr = urelatertFnr))

            identhendelseService.behandleIdenthendelse(
                id = UUID.randomUUID(),
                identhendelseDto = IdenthendelseDto(gammeltFnr = gammeltFnr.verdi, nyttFnr = nyttFnr.verdi),
            )

            sakRepo.hentForFnr(gammeltFnr) shouldBe null
            sakRepo.hentForFnr(nyttFnr) shouldBe sak.copy(fnr = nyttFnr)
            behandlingRepo.hentForFnr(gammeltFnr).firstOrNull() shouldBe null
            behandlingRepo.hentForFnr(nyttFnr).firstOrNull() shouldBe behandling.copy(fnr = nyttFnr)
            vedtakRepo.hentForFnr(gammeltFnr).firstOrNull() shouldBe null
            vedtakRepo.hentForFnr(nyttFnr).firstOrNull() shouldBe vedtak.copy(fnr = nyttFnr)

            sakRepo.hentForFnr(urelatertFnr) shouldNotBe null
            behandlingRepo.hentForFnr(urelatertFnr).firstOrNull() shouldNotBe null
            vedtakRepo.hentForFnr(urelatertFnr).firstOrNull() shouldNotBe null
        }
    }
}
