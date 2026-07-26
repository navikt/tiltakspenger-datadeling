package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakDetaljerResponse.RettighetResponseJson
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.satser.Satser
import org.junit.jupiter.api.Test

/**
 * Mappingen bak `POST /vedtak/detaljer`.
 * Stans og avslag filtreres normalt vekk før mappingen, så grenene for dem er defensive.
 */
class VedtakDetaljerResponseTest {

    private val log = KotlinLogging.logger {}
    private val idag = 15.januar(2024)
    private val sak = SakMother.sak()

    private fun medSak(vedtak: TiltakspengerVedtak) = TiltakspengeVedtakMedSak(sak = sak, vedtak = vedtak)

    @Test
    fun `avslag skal aldri na mappingen og kaster om det skjer`() {
        val avslag = medSak(VedtakMother.tiltakspengerVedtak(rettighet = TiltakspengerVedtak.Rettighet.AVSLAG))

        shouldThrow<IllegalStateException> { avslag.toVedtakDetaljerResponse(log, idag) }
    }

    @Test
    fun `bruker innvilgelsesperioden som fom og tom`() {
        val vedtak = medSak(
            VedtakMother.tiltakspengerVedtak(
                virkningsperiode = (1 til 31.januar(2024)),
                innvilgelsesperiode = (10 til 20.januar(2024)),
            ),
        )

        val respons = vedtak.toVedtakDetaljerResponse(log, idag)

        respons.fom shouldBe 10.januar(2024)
        respons.tom shouldBe 20.januar(2024)
    }

    @Test
    fun `faller tilbake pa virkningsperioden nar det ikke finnes en innvilgelsesperiode`() {
        val stans = medSak(
            VedtakMother.tiltakspengerVedtak(
                virkningsperiode = (1 til 31.januar(2024)),
                rettighet = TiltakspengerVedtak.Rettighet.STANS,
            ),
        )

        val respons = stans.toVedtakDetaljerResponse(log, idag)

        respons.fom shouldBe 1.januar(2024)
        respons.tom shouldBe 31.januar(2024)
        respons.rettighet shouldBe RettighetResponseJson.INGENTING
    }

    @Test
    fun `tiltakspenger med barnetillegg far satsen for barnetillegg, uten far den null`() {
        val medBarnetillegg = medSak(
            VedtakMother.tiltakspengerVedtak(rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG),
        ).toVedtakDetaljerResponse(log, idag)
        val utenBarnetillegg = medSak(
            VedtakMother.tiltakspengerVedtak(rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER),
        ).toVedtakDetaljerResponse(log, idag)

        medBarnetillegg.rettighet shouldBe RettighetResponseJson.TILTAKSPENGER_OG_BARNETILLEGG
        medBarnetillegg.satsBarnetillegg shouldBe Satser.sats(idag).satsBarnetillegg
        utenBarnetillegg.satsBarnetillegg shouldBe 0
    }
}
