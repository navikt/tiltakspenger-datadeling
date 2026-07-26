package no.nav.tiltakspenger.datadeling.vedtak

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

class MottaNyttVedtakServiceTest {

    private val sak = SakMother.sak()

    private fun mottattVedtak(sakId: SakId = sak.id) = MottattTiltakspengerVedtak(
        virkningsperiode = (1 til 31.januar(2024)),
        innvilgelsesperiode = (1 til 31.januar(2024)),
        omgjørRammevedtakId = null,
        omgjortAvRammevedtakId = null,
        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        vedtakId = "vedtakId",
        sakId = sakId,
        mottattTidspunkt = LocalDateTime.parse("2024-01-15T10:00:00"),
        opprettet = LocalDateTime.parse("2024-01-15T09:00:00"),
        barnetillegg = null,
        valgteHjemlerHarIkkeRettighet = null,
    )

    private fun repoer(): Pair<FakeVedtakRepo, FakeSakRepo> {
        val vedtakRepo = FakeVedtakRepo()
        val sakRepo = FakeSakRepo(vedtakRepo = vedtakRepo, behandlingRepo = FakeBehandlingRepo())
        return vedtakRepo to sakRepo
    }

    @Test
    fun `saken finnes - beriker vedtaket med saksnummer og fnr og lagrer det`() {
        val (vedtakRepo, sakRepo) = repoer()
        sakRepo.lagre(sak)

        MottaNyttVedtakService(vedtakRepo, sakRepo).motta(mottattVedtak()) shouldBe Unit.right()

        val lagret = vedtakRepo.alle().single()
        lagret.vedtakId shouldBe "vedtakId"
        lagret.saksnummer shouldBe sak.saksnummer
        lagret.fnr shouldBe sak.fnr
    }

    @Test
    fun `saken finnes ikke - gir SakIkkeFunnet og lagrer ingenting`() {
        val (vedtakRepo, sakRepo) = repoer()
        val ukjentSakId = SakId.random()

        MottaNyttVedtakService(vedtakRepo, sakRepo).motta(mottattVedtak(sakId = ukjentSakId)) shouldBe
            KanIkkeMottaVedtak.SakIkkeFunnet(ukjentSakId).left()

        vedtakRepo.alle() shouldBe emptyList()
    }

    @Test
    fun `oppslag av sak kaster - gir Persisteringsfeil`() {
        val vedtakRepo = FakeVedtakRepo()
        val sakRepo = mockk<SakRepo>()
        every { sakRepo.hentForId(any()) } throws RuntimeException("databasen er nede")

        MottaNyttVedtakService(vedtakRepo, sakRepo).motta(mottattVedtak()) shouldBe
            KanIkkeMottaVedtak.Persisteringsfeil.left()
    }

    @Test
    fun `lagring av vedtak kaster - gir Persisteringsfeil`() {
        val vedtakRepo = mockk<VedtakRepo>()
        every { vedtakRepo.lagre(any()) } throws RuntimeException("databasen er nede")
        val sakRepo = mockk<SakRepo>()
        every { sakRepo.hentForId(any()) } returns sak

        MottaNyttVedtakService(vedtakRepo, sakRepo).motta(mottattVedtak()) shouldBe
            KanIkkeMottaVedtak.Persisteringsfeil.left()
    }

    @Test
    fun `beriking med feil sak er en programmeringsfeil og kaster`() {
        shouldThrow<IllegalArgumentException> {
            mottattVedtak(sakId = SakId.random()).medSak(sak)
        }
    }
}
