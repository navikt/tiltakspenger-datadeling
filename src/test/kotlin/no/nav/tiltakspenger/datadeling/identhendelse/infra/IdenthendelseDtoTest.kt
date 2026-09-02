package no.nav.tiltakspenger.datadeling.identhendelse.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.objectMapper
import org.junit.jupiter.api.Test

/**
 * Meldingsformen på identhendelse-topicet.
 * Konsumenten deserialiserer denne, så feltnavnene er en kontrakt mot avsender.
 */
class IdenthendelseDtoTest {

    @Test
    fun `deserialiserer meldingen fra topicet`() {
        val dto = objectMapper.readValue(
            """{"gammeltFnr": "12845678910", "nyttFnr": "10987654321"}""",
            IdenthendelseDto::class.java,
        )

        dto shouldBe IdenthendelseDto(gammeltFnr = "12845678910", nyttFnr = "10987654321")
    }
}
