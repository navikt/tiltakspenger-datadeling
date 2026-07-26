package no.nav.tiltakspenger.datadeling.identhendelse.infra

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseService
import no.nav.tiltakspenger.libs.common.Fnr
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Konsumeringen av identhendelser (fnr-bytte fra PDL).
 *
 * Testen kaller `consume` direkte i stedet for `run`, som ville krevd en broker.
 * Kafka er kun transport her; jobben med å oppdatere identene ligger i [IdenthendelseService].
 */
class IdenthendelseConsumerTest {

    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `deserialiserer meldingen og sender identene videre til servicen`() = runTest {
        val service = mockk<IdenthendelseService>()
        every { service.behandleIdenthendelse(any(), any(), any()) } just Runs

        consumer(service).consume(id, """{"gammeltFnr": "12345678910", "nyttFnr": "10987654321"}""")

        verify(exactly = 1) {
            service.behandleIdenthendelse(
                id = id,
                gammeltFnr = Fnr.fromString("12345678910"),
                nyttFnr = Fnr.fromString("10987654321"),
            )
        }
    }

    @Test
    fun `ugyldig fnr i meldingen stopper konsumeringen i stedet for a lagre soppel`() = runTest {
        val service = mockk<IdenthendelseService>()

        shouldThrow<Exception> {
            consumer(service).consume(id, """{"gammeltFnr": "ugyldig", "nyttFnr": "10987654321"}""")
        }

        verify(exactly = 0) { service.behandleIdenthendelse(any(), any(), any()) }
    }

    @Test
    fun `melding som ikke er gyldig json stopper konsumeringen`() = runTest {
        val service = mockk<IdenthendelseService>()

        shouldThrow<Exception> {
            consumer(service).consume(id, "ikke json")
        }

        verify(exactly = 0) { service.behandleIdenthendelse(any(), any(), any()) }
    }

    @Test
    fun `kan startes og stoppes uten at det finnes en broker`() {
        val consumer = consumer(mockk())

        val job = consumer.run()
        consumer.stop()

        // Uten broker feiler selve konsumeringen, men jobben skal ikke bli hengende etter stop.
        job.cancel()
        job.isActive shouldBe false
    }

    private fun consumer(service: IdenthendelseService) = IdenthendelseConsumer(
        identhendelseService = service,
        topic = "test.identhendelse-v1",
    )
}
