import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.tp.TpClient
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.service.VedtakServiceImpl
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakServiceImplTest {

    private val TPClient = mockk<TpClient>()
    private val arenaClient = mockk<ArenaClient>()
    private val vedtakService = VedtakServiceImpl(TPClient, arenaClient)

    @Test
    fun `test hentVedtak`() {
        runBlocking {
            val ident = "01234567891"
            val fom = LocalDate.parse("2022-01-01")
            val tom = LocalDate.parse("2022-12-31")
            val expectedVedtakFraArena = listOf(
                Vedtak(
                    fom = fom,
                    tom = tom,
                    antallDager = 10.0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 0,
                    antallBarn = 0,
                    relaterteTiltak = "tiltak",
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "36475317",
                    sakId = "13297369",
                ),
            )

            val expectedVedtakFraVedtak = listOf(
                Vedtak(
                    fom = fom.plusDays(10),
                    tom = tom.plusDays(10),
                    antallDager = 20.0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 0,
                    antallBarn = 0,
                    relaterteTiltak = "tiltak",
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "987654",
                    sakId = "67676767",
                ),
            )

            coEvery { arenaClient.hentVedtak(ident, fom, tom) } returns expectedVedtakFraArena
            coEvery { TPClient.hentVedtak(ident, fom, tom) } returns expectedVedtakFraVedtak

            val result = vedtakService.hentVedtak(ident, fom, tom)

            result shouldContainExactlyInAnyOrder expectedVedtakFraVedtak + expectedVedtakFraArena
        }
    }
}
