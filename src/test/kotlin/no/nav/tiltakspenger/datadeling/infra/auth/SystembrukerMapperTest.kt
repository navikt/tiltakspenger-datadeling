package no.nav.tiltakspenger.datadeling.infra.auth

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import org.junit.jupiter.api.Test

/**
 * Mapper `roles`-claimet fra Entra ID til våre systembrukerroller.
 * Ukjente roller skal filtreres bort i stedet for å feile, slik at en ny rolle i Entra ID ikke låser ute klienten.
 */
class SystembrukerMapperTest {

    @Test
    fun `mapper alle kjente roller`() {
        val systembruker = systembrukerMapper(
            klientId = "id",
            klientnavn = "klientnavn",
            roller = setOf("lagre-tiltakspenger-hendelser", "les-vedtak", "les-behandling", "les-meldekort"),
        )

        systembruker.klientId shouldBe "id"
        systembruker.klientnavn shouldBe "klientnavn"
        systembruker.roller.value shouldBe setOf(
            Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER,
            Systembrukerrolle.LES_VEDTAK,
            Systembrukerrolle.LES_BEHANDLING,
            Systembrukerrolle.LES_MELDEKORT,
        )
    }

    @Test
    fun `filtrerer bort access_as_application, som alle Entra-klienter har`() {
        val systembruker = systembrukerMapper("id", "klientnavn", setOf("access_as_application", "les-vedtak"))

        systembruker.roller.value shouldBe setOf(Systembrukerrolle.LES_VEDTAK)
    }

    @Test
    fun `filtrerer bort ukjente roller i stedet for a feile`() {
        val systembruker = systembrukerMapper("id", "klientnavn", setOf("en-helt-ny-rolle", "les-meldekort"))

        systembruker.roller.value shouldBe setOf(Systembrukerrolle.LES_MELDEKORT)
    }

    @Test
    fun `token uten kjente roller gir systembruker uten roller`() {
        val systembruker = systembrukerMapper("id", "klientnavn", setOf("access_as_application"))

        systembruker.roller.value shouldBe emptySet()
    }
}
