package no.nav.tiltakspenger.datadeling.vedtak

import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import org.junit.jupiter.api.Test

/**
 * Jobben som deler rammevedtak med OBO (veilarbportefolje).
 * Kjører hvert minutt i prod, så den skal ikke gjøre noe når det ikke er noe å dele.
 */
class SendTilOboServiceTest {

    private val sak = SakMother.sak()

    @Test
    fun `ingen vedtak a dele - sender ingenting`() {
        val vedtakRepo = mockk<VedtakRepo>()
        every { vedtakRepo.hentRammevedtakSomSkalDelesMedObo(any()) } returns emptyList()
        val producer = mockk<OboYtelserProducer>()

        SendTilOboService(vedtakRepo, producer, fixedClock).send()

        verify(exactly = 0) { producer.sendTilObo(any(), any()) }
        verify(exactly = 0) { vedtakRepo.markerSendtTilObo(any(), any()) }
    }

    @Test
    fun `har vedtak a dele - sender hvert vedtak og markerer det som delt`() {
        val førsteVedtak = VedtakMother.tiltakspengerVedtak(vedtakId = "vedtak-1", fnr = sak.fnr)
        val andreVedtak = VedtakMother.tiltakspengerVedtak(vedtakId = "vedtak-2", fnr = sak.fnr)
        val vedtakRepo = mockk<VedtakRepo>()
        every { vedtakRepo.hentRammevedtakSomSkalDelesMedObo(any()) } returns listOf(
            TiltakspengeVedtakMedSak(sak = sak, vedtak = førsteVedtak),
            TiltakspengeVedtakMedSak(sak = sak, vedtak = andreVedtak),
        )
        every { vedtakRepo.markerSendtTilObo(any(), any()) } just Runs
        val producer = mockk<OboYtelserProducer>()
        every { producer.sendTilObo(any(), any()) } just Runs

        SendTilOboService(vedtakRepo, producer, fixedClock).send()

        // Markeringen skal skje etter utsendingen, slik at et vedtak heller sendes to ganger enn null.
        verifySequence {
            vedtakRepo.hentRammevedtakSomSkalDelesMedObo(any())
            producer.sendTilObo(sak.fnr, "vedtak-1")
            vedtakRepo.markerSendtTilObo("vedtak-1", nå(fixedClock))
            producer.sendTilObo(sak.fnr, "vedtak-2")
            vedtakRepo.markerSendtTilObo("vedtak-2", nå(fixedClock))
        }
    }

    @Test
    fun `bruker fnr fra saken, ikke fra vedtaket`() {
        val vedtakMedAnnetFnr = VedtakMother.tiltakspengerVedtak(
            vedtakId = "vedtak-1",
            fnr = Fnr.fromString("12345678902"),
        )
        // Forutsetningen testen hviler på: vedtaket bærer et annet fnr enn saken.
        vedtakMedAnnetFnr.fnr shouldNotBe sak.fnr

        val vedtakRepo = mockk<VedtakRepo>(relaxed = true)
        every { vedtakRepo.hentRammevedtakSomSkalDelesMedObo(any()) } returns listOf(
            TiltakspengeVedtakMedSak(sak = sak, vedtak = vedtakMedAnnetFnr),
        )
        val producer = mockk<OboYtelserProducer>(relaxed = true)

        SendTilOboService(vedtakRepo, producer, fixedClock).send()

        verify(exactly = 1) { producer.sendTilObo(sak.fnr, "vedtak-1") }
        verify(exactly = 0) { producer.sendTilObo(vedtakMedAnnetFnr.fnr, any()) }
    }
}
