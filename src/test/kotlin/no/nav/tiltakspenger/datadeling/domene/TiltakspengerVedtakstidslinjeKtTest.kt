package no.nav.tiltakspenger.datadeling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.felles.VedtakMother.tiltakspengerVedtak
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.rangeTo
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TiltakspengerVedtakstidslinjeKtTest {
    @Test
    fun `stanser siste del`() {
        val v1 = tiltakspengerVedtak(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            opprettetTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        )
        val v2 = tiltakspengerVedtak(
            fom = LocalDate.of(2024, 1, 15),
            tom = LocalDate.of(2024, 1, 31),
            opprettetTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.001"),
            rettighet = TiltakspengerVedtak.Rettighet.INGENTING,
        )
        listOf(v1, v2).toTidslinje() shouldBe Periodisering(
            PeriodeMedVerdi(v1, LocalDate.of(2024, 1, 1)..(LocalDate.of(2024, 1, 14))),
            PeriodeMedVerdi(v2, v2.periode),
        )
    }
}
