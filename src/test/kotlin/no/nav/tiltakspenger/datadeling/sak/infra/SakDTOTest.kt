package no.nav.tiltakspenger.datadeling.sak.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.Kilde
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Verifiserer de to mapperne fra domene/Arena til [SakDTO]. Default-verdiene
 * for `kilde` og `status` er det enkleste stedet å fange regresjon.
 */
class SakDTOTest {

    @Test
    fun `Sak til SakDTO - bruker TPSAK som kilde og Løpende som status`() {
        val sak = SakMother.sak(
            id = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
            saksnummer = "202401011001",
            opprettet = LocalDateTime.parse("2024-01-15T10:30:00"),
        )

        sak.toSakDTO() shouldBe SakDTO(
            sakId = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
            saksnummer = "202401011001",
            kilde = "TPSAK",
            status = "Løpende",
            opprettetDato = LocalDateTime.parse("2024-01-15T10:30:00"),
        )
    }

    @Test
    fun `ArenaVedtak Sak til SakDTO - bruker ARENA som kilde, beholder status og løfter dato til klokken ni`() {
        val arenaSak = ArenaVedtak(
            periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31)),
            rettighet = Rettighet.TILTAKSPENGER,
            vedtakId = "arenaVedtakId",
            kilde = Kilde.ARENA,
            fnr = Fnr.fromString("12345678910"),
            antallBarn = 0,
            dagsatsTiltakspenger = 285,
            dagsatsBarnetillegg = null,
            beslutningsdato = LocalDate.of(2024, 1, 5),
            sak = ArenaVedtak.Sak(
                sakId = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
                saksnummer = "202401011001",
                opprettetDato = LocalDate.of(2024, 1, 1),
                status = "Aktiv",
            ),
        ).sak

        arenaSak.toSakDTO() shouldBe SakDTO(
            sakId = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
            saksnummer = "202401011001",
            kilde = "ARENA",
            status = "Aktiv",
            opprettetDato = LocalDateTime.parse("2024-01-01T09:00:00"),
        )
    }
}
