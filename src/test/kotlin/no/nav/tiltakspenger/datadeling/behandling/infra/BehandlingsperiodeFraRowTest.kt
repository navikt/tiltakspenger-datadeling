package no.nav.tiltakspenger.datadeling.behandling.infra

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotliquery.Row
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Halve perioder avvises av check-constraintet i V33, så denne vakten kan ikke nås gjennom en ekte database.
 * Den testes derfor direkte, mot en rad som bryter invarianten.
 */
class BehandlingsperiodeFraRowTest {

    @Test
    fun `begge datoene satt gir en periode`() {
        behandlingsperiodeFraRow(row(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))) shouldBe
            Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
    }

    @Test
    fun `ingen av datoene satt gir ingen periode`() {
        behandlingsperiodeFraRow(row(null, null)) shouldBe null
    }

    @Test
    fun `kun fra og med satt avvises`() {
        shouldThrow<IllegalStateException> { behandlingsperiodeFraRow(row(LocalDate.of(2024, 1, 1), null)) }
    }

    @Test
    fun `kun til og med satt avvises`() {
        shouldThrow<IllegalStateException> { behandlingsperiodeFraRow(row(null, LocalDate.of(2024, 1, 31))) }
    }

    private fun row(fraOgMed: LocalDate?, tilOgMed: LocalDate?): Row = mockk {
        every { localDateOrNull("fra_og_med") } returns fraOgMed
        every { localDateOrNull("til_og_med") } returns tilOgMed
        every { string("behandling_id") } returns "behandlingId"
    }
}
