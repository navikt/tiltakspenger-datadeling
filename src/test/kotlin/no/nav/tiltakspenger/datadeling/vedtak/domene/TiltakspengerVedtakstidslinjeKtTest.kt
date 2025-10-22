package no.nav.tiltakspenger.datadeling.vedtak.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TiltakspengerVedtakstidslinjeKtTest {
    @Test
    fun `stanser siste del`() {
        val v1 = VedtakMother.tiltakspengerVedtak(
            virkningsperiode = 1 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        )
        val v2 = VedtakMother.tiltakspengerVedtak(
            virkningsperiode = 15 til 31.januar(2024),
            opprettetTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.001"),
            rettighet = TiltakspengerVedtak.Rettighet.STANS,
        )
        listOf(v1, v2).toTidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(v1, 1 til 14.januar(2024)),
            PeriodeMedVerdi(v2, v2.virkningsperiode),
        )
        listOf(v2, v1).toTidslinje() shouldBe Periodisering.Companion(
            PeriodeMedVerdi(v1, 1 til 14.januar(2024)),
            PeriodeMedVerdi(v2, v2.virkningsperiode),
        )
    }
}
