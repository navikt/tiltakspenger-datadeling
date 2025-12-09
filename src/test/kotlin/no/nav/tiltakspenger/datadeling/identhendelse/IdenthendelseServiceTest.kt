package no.nav.tiltakspenger.datadeling.identhendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.shouldBeCloseTo
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
            val behandling = BehandlingMother.tiltakspengerBehandling(sakId = sak.id)
            behandlingRepo.lagre(behandling, gammeltFnr, sak.saksnummer)
            val vedtak = VedtakMother.tiltakspengerVedtak(sakId = sak.id)
            vedtakRepo.lagre(vedtak, gammeltFnr, sak.saksnummer)
            val urelatertFnr = Fnr.random()
            val urelatertSak = SakMother.sak(id = "id2", saksnummer = "saksnummer2", fnr = urelatertFnr)
            sakRepo.lagre(urelatertSak)
            behandlingRepo.lagre(BehandlingMother.tiltakspengerBehandling(sakId = urelatertSak.id), urelatertFnr, urelatertSak.saksnummer)
            vedtakRepo.lagre(VedtakMother.tiltakspengerVedtak(sakId = urelatertSak.id), urelatertFnr, urelatertSak.saksnummer)

            identhendelseService.behandleIdenthendelse(
                id = UUID.randomUUID(),
                identhendelseDto = IdenthendelseDto(gammeltFnr = gammeltFnr.verdi, nyttFnr = nyttFnr.verdi),
            )

            sakRepo.hentForFnr(gammeltFnr) shouldBe null
            val sakFraDb = sakRepo.hentForFnr(nyttFnr)!!
            sakFraDb.id shouldBe sak.id
            sakFraDb.saksnummer shouldBe sak.saksnummer
            sakFraDb.opprettet shouldBeCloseTo sak.opprettet
            behandlingRepo.hentForFnr(gammeltFnr).firstOrNull() shouldBe null
            behandlingRepo.hentForFnr(nyttFnr).firstOrNull()?.behandling shouldBe behandling
            vedtakRepo.hentForFnr(gammeltFnr).firstOrNull() shouldBe null
            vedtakRepo.hentForFnr(nyttFnr).firstOrNull()?.vedtak shouldBe vedtak

            sakRepo.hentForFnr(urelatertFnr) shouldNotBe null
            behandlingRepo.hentForFnr(urelatertFnr).firstOrNull() shouldNotBe null
            vedtakRepo.hentForFnr(urelatertFnr).firstOrNull() shouldNotBe null
        }
    }
}
