package no.nav.tiltakspenger.datadeling.service

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.infra.db.VedtakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.til
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VedtakServiceTest {

    private val vedtakRepo = mockk<VedtakRepo>()
    private val arenaClient = mockk<ArenaClient>()
    private val vedtakService = VedtakService(vedtakRepo, arenaClient)

    private val periode2022 = 1.januar(2022) til 31.desember(2022)
    val ident = "01234567891"
    val fnr = Fnr.fromString(ident)

    @Test
    fun `enkelt innvilget vedtak`() {
        runBlocking {
            val systembruker = Systembruker(
                klientnavn = "testKlientnavn",
                klientId = "testKlientId",
                roller = Systembrukerroller(Systembrukerrolle.LES_VEDTAK),
            )
            val expectedVedtakFraVedtak = listOf(
                TiltakspengerVedtak(
                    periode = 1 til 31.januar(2022),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "987654",
                    sakId = "67676767",
                    saksnummer = "987654",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any(), Kilde.TPSAK) } returns expectedVedtakFraVedtak
            val result = vedtakService.hentTpVedtak(fnr, periode2022, systembruker).getOrFail()
            result shouldContainExactlyInAnyOrder expectedVedtakFraVedtak
        }
    }

    @Test
    fun `to innvilgelser med hull`() {
        runBlocking {
            val systembruker = Systembruker(
                klientnavn = "testKlientnavn",
                klientId = "testKlientId",
                roller = Systembrukerroller(Systembrukerrolle.LES_VEDTAK),
            )
            val expectedVedtakFraVedtak = listOf(
                TiltakspengerVedtak(
                    periode = (1 til 31.januar(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "987654",
                    sakId = "67676767",
                    saksnummer = "987654",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
                TiltakspengerVedtak(
                    periode = (1 til 31.mars(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "987654",
                    sakId = "67676767",
                    saksnummer = "987654",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-03-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-03-01T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any(), Kilde.TPSAK) } returns expectedVedtakFraVedtak
            val result = vedtakService.hentTpVedtak(fnr, periode2022, systembruker).getOrFail()
            result shouldContainExactlyInAnyOrder expectedVedtakFraVedtak
        }
    }

    @Test
    fun `innvilgelse til stans`() {
        runBlocking {
            val systembruker = Systembruker(
                klientnavn = "testKlientnavn",
                klientId = "testKlientId",
                roller = Systembrukerroller(Systembrukerrolle.LES_VEDTAK),
            )
            val vedtaksliste = listOf(
                TiltakspengerVedtak(
                    periode = (1.januar(2022) til 31.mars(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "v1",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
                TiltakspengerVedtak(
                    periode = (1.februar(2022) til 31.mars(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.INGENTING,
                    vedtakId = "v2",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any(), Kilde.TPSAK) } returns vedtaksliste
            val result = vedtakService.hentTpVedtak(fnr, periode2022, systembruker).getOrFail()
            result shouldBe listOf(
                TiltakspengerVedtak(
                    periode = (1.januar(2022) til 31.januar(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "v1",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
        }
    }

    @Test
    fun `stanser alle dager`() {
        runBlocking {
            val systembruker = Systembruker(
                klientnavn = "testKlientnavn",
                klientId = "testKlientId",
                roller = Systembrukerroller(Systembrukerrolle.LES_VEDTAK),
            )
            val vedtaksliste = listOf(
                TiltakspengerVedtak(
                    periode = (1.januar(2022) til 31.mars(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "v1",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
                TiltakspengerVedtak(
                    periode = (1.januar(2022) til 31.mars(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.INGENTING,
                    vedtakId = "v2",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any(), Kilde.TPSAK) } returns vedtaksliste
            val result = vedtakService.hentTpVedtak(fnr, periode2022, systembruker).getOrFail()
            result shouldBe emptyList()
        }
    }

    @Test
    fun `opphører midt i perioden`() {
        runBlocking {
            val systembruker = Systembruker(
                klientnavn = "testKlientnavn",
                klientId = "testKlientId",
                roller = Systembrukerroller(Systembrukerrolle.LES_VEDTAK),
            )
            val vedtaksliste = listOf(
                TiltakspengerVedtak(
                    periode = (1.januar(2022) til 31.mars(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "v1",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
                TiltakspengerVedtak(
                    periode = (1.februar(2022) til 28.februar(2022)),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.INGENTING,
                    vedtakId = "v2",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any(), Kilde.TPSAK) } returns vedtaksliste
            val result = vedtakService.hentTpVedtak(fnr, periode2022, systembruker).getOrFail()
            result shouldBe listOf(
                TiltakspengerVedtak(
                    periode = 1.januar(2022) til 31.januar(2022),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "v1",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
                TiltakspengerVedtak(
                    periode = 1.mars(2022) til 31.mars(2022),
                    antallDagerPerMeldeperiode = 10,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "v1",
                    sakId = "s1",
                    saksnummer = "sa1",
                    fnr = fnr,
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                ),
            )
        }
    }
}
