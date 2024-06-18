import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.vedtak.VedtakClient
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.service.VedtakServiceImpl
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakServiceImplTest {

    private val vedtakClient = mockk<VedtakClient>()
    private val arenaClient = mockk<ArenaClient>()
    private val vedtakService = VedtakServiceImpl(vedtakClient, arenaClient)

    @Test
    fun `test hentVedtak`() {
        // Denne testen gir mer mening når vi har en implementasjon av VedtakClient
        // og skal slå sammen resultater fra flere kilder
        runBlocking {
            val ident = "01234567891"
            val fom = LocalDate.parse("2022-01-01")
            val tom = LocalDate.parse("2022-12-31")
            val expectedVedtak = listOf(
                Vedtak(
                    fom = fom,
                    tom = tom,
                    antallDager = 10.0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 0,
                    antallBarn = 0,
                ),
            )

            coEvery { arenaClient.hentVedtak(ident, fom, tom) } returns expectedVedtak

            val result = vedtakService.hentVedtak(ident, fom, tom)

            result shouldBe expectedVedtak
        }
    }
}
