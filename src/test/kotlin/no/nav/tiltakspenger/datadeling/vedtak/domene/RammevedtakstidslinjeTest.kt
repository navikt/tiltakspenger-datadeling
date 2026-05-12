package no.nav.tiltakspenger.datadeling.vedtak.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RammevedtakstidslinjeTest {

    @Test
    fun `tom liste gir tom tidslinje`() {
        emptyList<TiltakspengerVedtak>().tilRammevedtakstidslinje() shouldBe Periodisering.empty()
        emptyList<TiltakspengerVedtak>().tilInnvilgetRammevedtakstidslinje() shouldBe Periodisering.empty()
    }

    @Test
    fun `avslag ekskluderes fra tidslinjen`() {
        val avslag = VedtakMother.tiltakspengerVedtak(
            virkningsperiode = 1 til 31.januar(2024),
            rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
        )
        listOf(avslag).tilRammevedtakstidslinje() shouldBe Periodisering.empty()
        listOf(avslag).tilInnvilgetRammevedtakstidslinje() shouldBe Periodisering.empty()
    }

    @Test
    fun `vedtak som er omgjort i sin helhet ekskluderes - omgjort til ny innvilgelse`() {
        // I prod kommer alle vedtakene på saken: det opprinnelige vedtaket (med omgjortAvRammevedtakId
        // satt fordi det er HELT omgjort av nøyaktig ett senere vedtak) og selve omgjøringsvedtaket.
        val opprinnelig = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v1",
            virkningsperiode = 1 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            omgjortAvRammevedtakId = "v2",
        )
        val helOmgjoering = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v2",
            virkningsperiode = 1 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-02-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            omgjørRammevedtakId = "v1",
        )

        listOf(opprinnelig, helOmgjoering).tilRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(helOmgjoering, 1 til 31.januar(2024)),
        )
        listOf(opprinnelig, helOmgjoering).tilInnvilgetRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(helOmgjoering, 1 til 31.januar(2024)),
        )
    }

    @Test
    fun `stans erstatter siste del av innvilgelse paa tidslinjen, men ekskluderes fra innvilget`() {
        val innvilgelse = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v1",
            virkningsperiode = 1 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        )
        // Stans kommer fra en revurdering (ikke omgjøring), så ingen omgjør-/omgjortAv-relasjon.
        val stans = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v2",
            virkningsperiode = 15 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.001"),
            rettighet = TiltakspengerVedtak.Rettighet.STANS,
        )

        listOf(innvilgelse, stans).tilRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(innvilgelse, 1 til 14.januar(2024)),
            PeriodeMedVerdi(stans, stans.virkningsperiode),
        )

        listOf(innvilgelse, stans).tilInnvilgetRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(innvilgelse, 1 til 14.januar(2024)),
        )
    }

    @Test
    fun `delvis opphoer (omgjoering) ekskluderes fra innvilget tidslinje`() {
        // OPPHØR er i saksbehandling-api en OmgjøringOpphør, så den har omgjørRammevedtakId satt.
        // Når den kun dekker deler av det opprinnelige vedtaket, blir det DELVIS-omgjøring og
        // det opprinnelige vedtaket har omgjortAvRammevedtakId = null.
        val innvilgelse = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v1",
            virkningsperiode = 1 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        )
        val opphoer = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v2",
            virkningsperiode = 20 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-02-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.OPPHØR,
            omgjørRammevedtakId = "v1",
        )

        listOf(innvilgelse, opphoer).tilRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(innvilgelse, 1 til 19.januar(2024)),
            PeriodeMedVerdi(opphoer, 20 til 31.januar(2024)),
        )

        listOf(innvilgelse, opphoer).tilInnvilgetRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(innvilgelse, 1 til 19.januar(2024)),
        )
    }

    @Test
    fun `omgjoeringsvedtak med kortere innvilgelsesperiode enn virkningsperiode`() {
        // Et omgjøringsvedtak har minimum samme virkningsperiode som det vedtaket det omgjør,
        // men kan ha en kortere innvilgelsesperiode (resten er implisitt opphør).
        // I prod kommer både det opprinnelige vedtaket (HELT omgjort) og selve omgjøringen.
        val opprinnelig = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v1",
            virkningsperiode = 1 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            omgjortAvRammevedtakId = "v2",
        )
        val omgjoering = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v2",
            virkningsperiode = 1 til 31.januar(2024),
            innvilgelsesperiode = 1 til 15.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-02-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            omgjørRammevedtakId = "v1",
        )

        listOf(opprinnelig, omgjoering).tilRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(omgjoering, 1 til 31.januar(2024)),
        )
        // 16.-31. januar er implisitt opphørt (innenfor virkningsperioden, men utenfor innvilgelsesperioden).
        listOf(opprinnelig, omgjoering).tilInnvilgetRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(omgjoering, 1 til 15.januar(2024)),
        )
    }

    @Test
    fun `delvis omgjoering - kun deler av et vedtak er omgjort til opphoer`() {
        // Saksbehandling-api setter omgjortAvRammevedtakId kun ved en HEL omgjøring av nøyaktig ett vedtak.
        // Ved delvis omgjøring kommer det opprinnelige vedtaket med omgjortAvRammevedtakId = null,
        // og det er det nye omgjøringsvedtaket som via toTidslinje() "overskriver" den omgjorte delen.
        val opprinnelig = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v1",
            virkningsperiode = 1 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        )
        val delvisOpphoer = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v2",
            virkningsperiode = 1 til 15.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-02-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.OPPHØR,
            omgjørRammevedtakId = "v1",
        )

        listOf(opprinnelig, delvisOpphoer).tilRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(delvisOpphoer, 1 til 15.januar(2024)),
            PeriodeMedVerdi(opprinnelig, 16 til 31.januar(2024)),
        )

        listOf(opprinnelig, delvisOpphoer).tilInnvilgetRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(opprinnelig, 16 til 31.januar(2024)),
        )
    }

    @Test
    fun `to delvise omgjoeringer dekker hele det opprinnelige vedtaket`() {
        // Vedtak 1 (innvilgelse 1.-31. januar) blir delvis omgjort til opphør av vedtak 2 (1.-15.),
        // og resten omgjort til opphør av vedtak 3 (16.-31.). I saksbehandling-api blir
        // omgjortAvRammevedtakId fortsatt null på det opprinnelige vedtaket (fordi det er flere
        // omgjøringer / DELVIS-grad), men toTidslinje() skal "overskrive" hele perioden med v2 og v3.
        val v1 = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v1",
            virkningsperiode = 1 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        )
        val v2 = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v2",
            virkningsperiode = 1 til 15.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-02-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.OPPHØR,
            omgjørRammevedtakId = "v1",
        )
        val v3 = VedtakMother.tiltakspengerVedtak(
            vedtakId = "v3",
            virkningsperiode = 16 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2024-03-01T00:00:00"),
            rettighet = TiltakspengerVedtak.Rettighet.OPPHØR,
            omgjørRammevedtakId = "v1",
        )

        listOf(v1, v2, v3).tilRammevedtakstidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(v2, 1 til 15.januar(2024)),
            PeriodeMedVerdi(v3, 16 til 31.januar(2024)),
        )

        listOf(v1, v2, v3).tilInnvilgetRammevedtakstidslinje() shouldBe Periodisering.empty()
    }
}
