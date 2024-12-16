package no.nav.tiltakspenger.datadeling.service

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.app.VedtakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakServiceTest {

    private val vedtakRepo = mockk<VedtakRepo>()
    private val arenaClient = mockk<ArenaClient>()
    private val vedtakService = VedtakService(vedtakRepo, arenaClient)

    @Test
    fun `test hentVedtak`() {
        runBlocking {
            val ident = "01234567891"
            val fnr = Fnr.fromString(ident)
            val fom = LocalDate.parse("2022-01-01")
            val tom = LocalDate.parse("2022-12-31")
            val periode = Periode(fom, tom)
            val systembruker =
                Systembruker(klientnavn = "testKlientnavn", klientId = "testKlientId", roller = Systembrukerroller(Systembrukerrolle.LES_VEDTAK))

            val expectedVedtakFraVedtak = listOf(
                TiltakspengerVedtak(
                    periode = Periode(fom.plusDays(10), tom.plusDays(10)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "987654",
                    sakId = "67676767",
                    saksnummer = "987654",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                ),
            )

            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, periode, "tp") } returns expectedVedtakFraVedtak

            val result = vedtakService.hentTpVedtak(fnr, periode, systembruker).getOrFail()

            result shouldContainExactlyInAnyOrder Periodisering(expectedVedtakFraVedtak)
        }
    }
}
