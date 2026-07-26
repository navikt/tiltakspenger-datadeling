package no.nav.tiltakspenger.datadeling.vedtak.infra.kafka

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.kafka.Producer
import org.junit.jupiter.api.Test

/**
 * Meldingen på `obo.ytelser-v1` er kun en trigger — veilarbportefolje henter selve dataene via REST.
 * Formen er likevel en kontrakt mot dem, så den asserteres som JSON.
 */
class OboYtelserKafkaProducerTest {

    @Test
    fun `sender melding med fnr som noekkel og fast meldingsform`() {
        val kafkaProducer = mockk<Producer<String, String>>()
        val topic = slot<String>()
        val key = slot<String>()
        val value = slot<String>()
        every { kafkaProducer.produce(capture(topic), capture(key), capture(value)) } just Runs

        OboYtelserKafkaProducer(kafkaProducer, "obo.ytelser-v1")
            .sendTilObo(Fnr.fromString("12345678910"), "vedtakId")

        topic.captured shouldBe "obo.ytelser-v1"
        key.captured shouldBe "12345678910"
        value.captured.shouldEqualJson(
            // language=JSON
            """
            {
              "personId": "12345678910",
              "meldingstype": "OPPRETT",
              "ytelsestype": "TILTAKSPENGER",
              "kildesystem": "TPSAK"
            }
            """.trimIndent(),
        )
    }
}
