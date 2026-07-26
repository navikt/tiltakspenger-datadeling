package no.nav.tiltakspenger.datadeling.vedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.Kilde
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * `POST /vedtak/perioder` skal aldri returnere avslag — de filtreres vekk i servicen.
 * Mappingen har derfor defensive grener som skal kaste hvis et avslag likevel skulle nå hit.
 */
class DatadelingsvedtakUtenAvslagTest {

    private val log = KotlinLogging.logger {}
    private val idag = 15.januar(2024)

    @Test
    fun `avslag skal aldri na mappingen og kaster om det skjer`() {
        val avslag = VedtakMother.tiltakspengerVedtak(rettighet = TiltakspengerVedtak.Rettighet.AVSLAG)

        shouldThrow<IllegalStateException> {
            avslag.toDatadelingsvedtakUtenAvslag(log, idag)
        }
    }

    @Test
    fun `stans mappes til INGENTING med virkningsperioden som periode`() {
        val stans = VedtakMother.tiltakspengerVedtak(
            virkningsperiode = (1 til 31.januar(2024)),
            rettighet = TiltakspengerVedtak.Rettighet.STANS,
        )

        val resultat = stans.toDatadelingsvedtakUtenAvslag(log, idag)

        resultat.rettighet shouldBe DatadelingsvedtakUtenAvslag.Rettighet.INGENTING
        resultat.periode shouldBe (1 til 31.januar(2024))
        resultat.sats shouldBe null
        resultat.innvilgelsesperioder shouldBe emptyList()
    }

    @Test
    fun `tiltakspenger med barnetillegg far satsen for barnetillegg, uten far den null`() {
        val medBarnetillegg = VedtakMother.tiltakspengerVedtak(
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
        ).toDatadelingsvedtakUtenAvslag(log, idag)
        val utenBarnetillegg = VedtakMother.tiltakspengerVedtak(
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
        ).toDatadelingsvedtakUtenAvslag(log, idag)

        medBarnetillegg.rettighet shouldBe DatadelingsvedtakUtenAvslag.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
        medBarnetillegg.satsBarnetillegg shouldBe Satser.sats(idag).satsBarnetillegg
        utenBarnetillegg.satsBarnetillegg shouldBe 0
    }

    @Test
    fun `arena-vedtak far vedtakstidspunkt klokka ni pa beslutningsdatoen`() {
        val arenaVedtak = arenaVedtak(beslutningsdato = LocalDate.of(2024, 1, 10))

        val resultat = arenaVedtak.toDatadelingsvedtakUtenAvslag()

        resultat.vedtakstidspunkt shouldBe
            LocalDate.of(2024, 1, 10).atTime(9, 0).atZone(zoneIdOslo).toOffsetDateTime()
    }

    @Test
    fun `arena-vedtak uten beslutningsdato far ingen vedtakstidspunkt`() {
        val arenaVedtak = arenaVedtak(beslutningsdato = null)

        arenaVedtak.toDatadelingsvedtakUtenAvslag().vedtakstidspunkt shouldBe null
    }

    private fun arenaVedtak(beslutningsdato: LocalDate?) = ArenaVedtak(
        periode = (1 til 31.januar(2024)),
        rettighet = Rettighet.TILTAKSPENGER,
        vedtakId = "arena-1",
        kilde = Kilde.ARENA,
        fnr = Fnr.fromString("12345678901"),
        antallBarn = 0,
        dagsatsTiltakspenger = 285,
        dagsatsBarnetillegg = 0,
        beslutningsdato = beslutningsdato,
        sak = ArenaVedtak.Sak(
            sakId = "arena-sak-1",
            saksnummer = "202401011001",
            opprettetDato = LocalDate.of(2024, 1, 1),
            status = "AKTIV",
        ),
    )
}
