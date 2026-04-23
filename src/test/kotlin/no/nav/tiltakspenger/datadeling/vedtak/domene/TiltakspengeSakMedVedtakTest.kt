package no.nav.tiltakspenger.datadeling.vedtak.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TiltakspengeSakMedVedtakTest {

    private val t1 = LocalDateTime.parse("2024-01-01T10:00:00")
    private val t2 = LocalDateTime.parse("2024-01-02T10:00:00")
    private val t3 = LocalDateTime.parse("2024-01-03T10:00:00")

    @Test
    fun `iverksattSøknadsbehandlingTidspunkt er opprettet-tidspunkt for TILTAKSPENGER`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(
            rettighet = Rettighet.TILTAKSPENGER,
            opprettetTidspunkt = t1,
        )
        TiltakspengeSakMedVedtak(
            sak = SakMother.sak(),
            vedtak = listOf(vedtak),
        ).iverksattSøknadsbehandlingTidspunkt shouldBe t1
    }

    @Test
    fun `iverksattSøknadsbehandlingTidspunkt er opprettet-tidspunkt for TILTAKSPENGER_OG_BARNETILLEGG`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(
            rettighet = Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            opprettetTidspunkt = t1,
        )
        TiltakspengeSakMedVedtak(
            sak = SakMother.sak(),
            vedtak = listOf(vedtak),
        ).iverksattSøknadsbehandlingTidspunkt shouldBe t1
    }

    @Test
    fun `iverksattSøknadsbehandlingTidspunkt er null for STANS`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(
            rettighet = Rettighet.STANS,
            opprettetTidspunkt = t1,
        )
        TiltakspengeSakMedVedtak(
            sak = SakMother.sak(),
            vedtak = listOf(vedtak),
        ).iverksattSøknadsbehandlingTidspunkt shouldBe null
    }

    @Test
    fun `iverksattSøknadsbehandlingTidspunkt er null for AVSLAG`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(
            rettighet = Rettighet.AVSLAG,
            opprettetTidspunkt = t1,
        )
        TiltakspengeSakMedVedtak(
            sak = SakMother.sak(),
            vedtak = listOf(vedtak),
        ).iverksattSøknadsbehandlingTidspunkt shouldBe null
    }

    @Test
    fun `iverksattSøknadsbehandlingTidspunkt er null for OPPHØR`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(
            rettighet = Rettighet.OPPHØR,
            opprettetTidspunkt = t1,
        )
        TiltakspengeSakMedVedtak(
            sak = SakMother.sak(),
            vedtak = listOf(vedtak),
        ).iverksattSøknadsbehandlingTidspunkt shouldBe null
    }

    @Test
    fun `iverksattSøknadsbehandlingTidspunkt er null når det ikke finnes vedtak`() {
        TiltakspengeSakMedVedtak(
            sak = SakMother.sak(),
            vedtak = emptyList(),
        ).iverksattSøknadsbehandlingTidspunkt shouldBe null
    }

    @Test
    fun `iverksattSøknadsbehandlingTidspunkt velger tidligste innvilgelsesvedtak selv om avslag er først`() {
        val avslag = VedtakMother.tiltakspengerVedtak(
            rettighet = Rettighet.AVSLAG,
            opprettetTidspunkt = t1,
        )
        val innvilgelse = VedtakMother.tiltakspengerVedtak(
            rettighet = Rettighet.TILTAKSPENGER,
            opprettetTidspunkt = t2,
        )
        val senereInnvilgelse = VedtakMother.tiltakspengerVedtak(
            rettighet = Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            opprettetTidspunkt = t3,
        )
        TiltakspengeSakMedVedtak(
            sak = SakMother.sak(),
            vedtak = listOf(senereInnvilgelse, avslag, innvilgelse),
        ).iverksattSøknadsbehandlingTidspunkt shouldBe t2
    }
}
