package no.nav.tiltakspenger.datadeling.vedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.satser.Satser
import org.junit.jupiter.api.Test

class TiltakspengerVedtakTest {

    private val log = KotlinLogging.logger {}

    @Test
    fun `krymping utenfor gammel virkningsperiode er en programmeringsfeil`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(virkningsperiode = (10 til 20.januar(2024)))

        shouldThrow<IllegalArgumentException> {
            vedtak.krympVirkningsperiode(1 til 31.januar(2024))
        }
    }

    @Test
    fun `krymping av innvilget vedtak krymper ogsa innvilgelsesperioden og barnetillegget`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(
            virkningsperiode = (1 til 31.januar(2024)),
            innvilgelsesperiode = (1 til 31.januar(2024)),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            barnetillegg = Barnetillegg(
                perioder = listOf(BarnetilleggPeriode(antallBarn = 1, periode = (1 til 31.januar(2024)))),
            ),
        )

        val krympet = vedtak.krympVirkningsperiode(10 til 20.januar(2024))

        krympet.virkningsperiode shouldBe (10 til 20.januar(2024))
        krympet.innvilgelsesperiode shouldBe (10 til 20.januar(2024))
        krympet.barnetillegg!!.perioder.single().periode shouldBe (10 til 20.januar(2024))
    }

    @Test
    fun `krymping av stans krymper virkningsperioden og lar innvilgelsesperioden vaere`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(
            virkningsperiode = (1 til 31.januar(2024)),
            rettighet = TiltakspengerVedtak.Rettighet.STANS,
            barnetillegg = Barnetillegg(
                perioder = listOf(BarnetilleggPeriode(antallBarn = 1, periode = (1 til 31.januar(2024)))),
            ),
        )

        val krympet = vedtak.krympVirkningsperiode(10 til 20.januar(2024))

        krympet.virkningsperiode shouldBe (10 til 20.januar(2024))
        krympet.innvilgelsesperiode shouldBe null
        krympet.barnetillegg!!.perioder.single().periode shouldBe (10 til 20.januar(2024))
    }

    @Test
    fun `satser hentes for foerste dag i innvilgelsesperioden naar den ligger fram i tid`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(virkningsperiode = (10 til 20.januar(2024)))

        vedtak.getSatser(log, 1.januar(2024)) shouldBe Satser.sats(10.januar(2024))
    }

    @Test
    fun `satser hentes for siste dag i innvilgelsesperioden naar den ligger tilbake i tid`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(virkningsperiode = (10 til 20.januar(2024)))

        vedtak.getSatser(log, 31.januar(2024)) shouldBe Satser.sats(20.januar(2024))
    }

    @Test
    fun `satser hentes for dagen i dag naar den er inne i innvilgelsesperioden`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(virkningsperiode = (10 til 20.januar(2024)))

        vedtak.getSatser(log, 15.januar(2024)) shouldBe Satser.sats(15.januar(2024))
    }

    @Test
    fun `stans har ingen satser`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(rettighet = TiltakspengerVedtak.Rettighet.STANS)

        vedtak.getSatser(log, 15.januar(2024)) shouldBe null
    }
}
