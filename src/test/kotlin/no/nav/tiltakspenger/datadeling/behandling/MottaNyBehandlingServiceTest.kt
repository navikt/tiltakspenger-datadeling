package no.nav.tiltakspenger.datadeling.behandling

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.fakes.FakeBehandlingRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeSakRepo
import no.nav.tiltakspenger.datadeling.fakes.FakeVedtakRepo
import no.nav.tiltakspenger.datadeling.sak.SakRepo
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MottaNyBehandlingServiceTest {

    private val sak = SakMother.sak()

    private fun mottattBehandling(sakId: SakId = sak.id) = MottattTiltakspengerBehandling(
        behandlingId = "behandlingId",
        sakId = sakId,
        periode = (1 til 31.januar(2024)),
        behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING,
        saksbehandler = "Z12345",
        beslutter = null,
        iverksattTidspunkt = null,
        opprettetTidspunktSaksbehandlingApi = LocalDateTime.parse("2024-01-15T09:00:00"),
        mottattTidspunktDatadeling = LocalDateTime.parse("2024-01-15T10:00:00"),
        behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
        sistEndret = LocalDateTime.parse("2024-01-15T10:00:00"),
    )

    private fun repoer(): Pair<FakeBehandlingRepo, FakeSakRepo> {
        val behandlingRepo = FakeBehandlingRepo()
        val sakRepo = FakeSakRepo(vedtakRepo = FakeVedtakRepo(), behandlingRepo = behandlingRepo)
        return behandlingRepo to sakRepo
    }

    @Test
    fun `saken finnes - beriker behandlingen med saksnummer og fnr og lagrer den`() {
        val (behandlingRepo, sakRepo) = repoer()
        sakRepo.lagre(sak)

        MottaNyBehandlingService(behandlingRepo, sakRepo).motta(mottattBehandling()) shouldBe Unit.right()

        val lagret = behandlingRepo.alle().single()
        lagret.behandlingId shouldBe "behandlingId"
        lagret.saksnummer shouldBe sak.saksnummer
        lagret.fnr shouldBe sak.fnr
    }

    @Test
    fun `saken finnes ikke - gir SakIkkeFunnet`() {
        val (behandlingRepo, sakRepo) = repoer()
        val ukjentSakId = SakId.random()

        MottaNyBehandlingService(behandlingRepo, sakRepo).motta(mottattBehandling(sakId = ukjentSakId)) shouldBe
            KanIkkeMottaBehandling.SakIkkeFunnet(ukjentSakId).left()
    }

    @Test
    fun `oppslag av sak kaster - gir Persisteringsfeil`() {
        val (behandlingRepo, _) = repoer()
        val sakRepo = mockk<SakRepo>()
        every { sakRepo.hentForId(any()) } throws RuntimeException("databasen er nede")

        MottaNyBehandlingService(behandlingRepo, sakRepo).motta(mottattBehandling()) shouldBe
            KanIkkeMottaBehandling.Persisteringsfeil.left()
    }

    @Test
    fun `lagring av behandling kaster - gir Persisteringsfeil`() {
        val behandlingRepo = mockk<BehandlingRepo>()
        every { behandlingRepo.lagre(any()) } throws RuntimeException("databasen er nede")
        val sakRepo = mockk<SakRepo>()
        every { sakRepo.hentForId(any()) } returns sak

        MottaNyBehandlingService(behandlingRepo, sakRepo).motta(mottattBehandling()) shouldBe
            KanIkkeMottaBehandling.Persisteringsfeil.left()
    }

    @Test
    fun `beriking med feil sak er en programmeringsfeil og kaster`() {
        shouldThrow<IllegalArgumentException> {
            mottattBehandling(sakId = SakId.random()).medSak(sak)
        }
    }
}
