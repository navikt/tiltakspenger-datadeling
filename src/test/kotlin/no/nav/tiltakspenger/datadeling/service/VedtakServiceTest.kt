package no.nav.tiltakspenger.datadeling.service

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.tp.TpClient
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.getOrFail
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakServiceTest {

    private val TPClient = mockk<TpClient>()
    private val arenaClient = mockk<ArenaClient>()
    private val vedtakService = VedtakService(TPClient, arenaClient)

    @Test
    fun `test hentVedtak`() {
        runBlocking {
            val ident = "01234567891"
            val fnr = Fnr.fromString(ident)
            val fom = LocalDate.parse("2022-01-01")
            val tom = LocalDate.parse("2022-12-31")
            val systembruker = Systembruker(brukernavn = "systembrukerNavn", roller = Systembrukerroller(Systembrukerrolle.LES_VEDTAK))
            val expectedVedtakFraArena = listOf(
                Vedtak(
                    fom = fom,
                    tom = tom,
                    antallDager = 10.0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 0,
                    antallBarn = 0,
                    tiltaksgjennomføringId = "tiltak",
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "36475317",
                    sakId = "13297369",
                    saksnummer = "36475317",
                    kilde = "tp",
                    fnr = fnr,
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
                    tiltaksgjennomføringId = "tiltak",
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "987654",
                    sakId = "67676767",
                    saksnummer = "987654",
                    kilde = "tp",
                    fnr = fnr,
                ),
            )

            coEvery { arenaClient.hentVedtak(ident, fom, tom) } returns expectedVedtakFraArena
            coEvery { TPClient.hentVedtak(ident, fom, tom) } returns expectedVedtakFraVedtak

            val result = vedtakService.hentVedtak(ident, fom, tom, systembruker).getOrFail()

            result shouldContainExactlyInAnyOrder expectedVedtakFraVedtak + expectedVedtakFraArena
        }
    }
}
