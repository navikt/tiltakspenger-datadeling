package no.nav.tiltakspenger.datadeling.vedtak.datadeling

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
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
    fun `hentTpVedtak - enkelt innvilget vedtak`() {
        runBlocking {
            val expectedVedtakFraVedtak = listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = 1 til 31.januar(2022),
                        innvilgelsesperiode = 1 til 31.januar(2022),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "987654",
                        sakId = "67676767",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "67676767",
                        saksnummer = "987654",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any()) } returns expectedVedtakFraVedtak
            val result = vedtakService.hentTpVedtak(fnr, periode2022)
            result shouldContainExactlyInAnyOrder expectedVedtakFraVedtak
        }
    }

    @Test
    fun `hentTpVedtak - to innvilgelser med hull`() {
        runBlocking {
            val expectedVedtakFraVedtak = listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = 1 til 31.januar(2022),
                        innvilgelsesperiode = 1 til 31.januar(2022),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "987654",
                        sakId = "67676767",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "67676767",
                        saksnummer = "987654",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = (1 til 31.mars(2022)),
                        innvilgelsesperiode = 1 til 31.mars(2022),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "987654",
                        sakId = "67676767",
                        mottattTidspunkt = LocalDateTime.parse("2021-03-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-03-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "67676767",
                        saksnummer = "987654",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any()) } returns expectedVedtakFraVedtak
            val result = vedtakService.hentTpVedtak(fnr, periode2022)
            result shouldContainExactlyInAnyOrder expectedVedtakFraVedtak
        }
    }

    @Test
    fun `hentTpVedtak - innvilgelse til stans`() {
        runBlocking {
            val vedtaksliste = listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = (1.januar(2022) til 31.mars(2022)),
                        innvilgelsesperiode = (1.januar(2022) til 31.mars(2022)),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = (1.februar(2022) til 31.mars(2022)),
                        innvilgelsesperiode = null,
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.STANS,
                        vedtakId = "v2",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK),
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any()) } returns vedtaksliste
            val result = vedtakService.hentTpVedtak(fnr, periode2022)
            result shouldBe listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = (1.januar(2022) til 31.januar(2022)),
                        innvilgelsesperiode = (1.januar(2022) til 31.januar(2022)),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
        }
    }

    @Test
    fun `hentTpVedtak - stanser alle dager`() {
        runBlocking {
            val vedtaksliste = listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = (1.januar(2022) til 31.mars(2022)),
                        innvilgelsesperiode = (1.januar(2022) til 31.mars(2022)),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = (1.januar(2022) til 31.mars(2022)),
                        innvilgelsesperiode = null,
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.STANS,
                        vedtakId = "v2",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK),
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any()) } returns vedtaksliste
            val result = vedtakService.hentTpVedtak(fnr, periode2022)
            result shouldBe emptyList()
        }
    }

    @Test
    fun `hentTpVedtak - opphører midt i perioden`() {
        runBlocking {
            val vedtaksliste = listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = (1.januar(2022) til 31.mars(2022)),
                        innvilgelsesperiode = (1.januar(2022) til 31.mars(2022)),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = (1.februar(2022) til 28.februar(2022)),
                        innvilgelsesperiode = null,
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.STANS,
                        vedtakId = "v2",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2022-01-02T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK),
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any()) } returns vedtaksliste
            val result = vedtakService.hentTpVedtak(fnr, periode2022)
            result shouldBe listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = 1.januar(2022) til 31.januar(2022),
                        innvilgelsesperiode = 1.januar(2022) til 31.januar(2022),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = 1.mars(2022) til 31.mars(2022),
                        innvilgelsesperiode = 1.mars(2022) til 31.mars(2022),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = "s1",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                    ),
                    sak = Sak(
                        id = "s1",
                        saksnummer = "sa1",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
        }
    }

    @Test
    fun `hentTpVedtak - avslag - returnerer tom liste`() {
        runBlocking {
            val expectedVedtakFraVedtak = listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = 1 til 31.januar(2022),
                        innvilgelsesperiode = null,
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                        vedtakId = "987654",
                        sakId = "67676767",
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.FREMMET_FOR_SENT),
                    ),
                    sak = Sak(
                        id = "67676767",
                        saksnummer = "987654",
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any()) } returns expectedVedtakFraVedtak
            val result = vedtakService.hentTpVedtak(fnr, periode2022)
            result shouldBe emptyList()
        }
    }

    @Test
    fun `hentTpVedtak - enkelt innvilget vedtak og avslag - avslag filtreres bort`() {
        runBlocking {
            val innvilgetVedtak = TiltakspengeVedtakMedSak(
                vedtak = TiltakspengerVedtak(
                    virkningsperiode = 1 til 31.januar(2022),
                    innvilgelsesperiode = 1 til 31.januar(2022),
                    omgjørRammevedtakId = null,
                    omgjortAvRammevedtakId = null,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "987654",
                    sakId = "67676767",
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                    valgteHjemlerHarIkkeRettighet = null,
                ),
                sak = Sak(
                    id = "67676767",
                    saksnummer = "987654",
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                ),
            )
            val avslag = TiltakspengeVedtakMedSak(
                vedtak = TiltakspengerVedtak(
                    virkningsperiode = 10 til 31.januar(2022),
                    innvilgelsesperiode = null,
                    omgjørRammevedtakId = null,
                    omgjortAvRammevedtakId = null,
                    rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                    vedtakId = "987654123",
                    sakId = "67676767",
                    mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                    barnetillegg = null,
                    valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.LIVSOPPHOLDSYTELSER),
                ),
                sak = Sak(
                    id = "67676767",
                    saksnummer = "987654",
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                ),
            )
            val expectedVedtakFraVedtak = listOf(
                innvilgetVedtak,
                avslag,
            )
            coEvery { vedtakRepo.hentForFnrOgPeriode(fnr, any()) } returns expectedVedtakFraVedtak
            val result = vedtakService.hentTpVedtak(fnr, periode2022)
            result shouldContainExactlyInAnyOrder listOf(innvilgetVedtak)
        }
    }
}
