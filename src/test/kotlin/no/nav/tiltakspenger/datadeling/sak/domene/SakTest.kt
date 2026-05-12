package no.nav.tiltakspenger.datadeling.sak.domene

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class SakTest {

    private fun fastClock(dato: LocalDate): Clock =
        Clock.fixed(dato.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    // region init-validering
    @Test
    fun `kaster dersom rammevedtak har annen sakId`() {
        val vedtak = VedtakMother.tiltakspengerVedtak(sakId = "annenSak")
        shouldThrow<IllegalArgumentException> {
            SakMother.sak(id = "sakId", rammevedtak = listOf(vedtak))
        }
    }

    @Test
    fun `kaster dersom behandling har annen sakId`() {
        val behandling = BehandlingMother.tiltakspengerBehandling(sakId = "annenSak")
        shouldThrow<IllegalArgumentException> {
            SakMother.sak(id = "sakId", behandlinger = listOf(behandling))
        }
    }
    // endregion

    // region erLøpende
    @Test
    fun `erLoepende er false naar saken ikke har vedtak`() {
        SakMother.sak().erLøpende(Clock.systemUTC()) shouldBe false
    }

    @Test
    fun `erLoepende er true naar dagens dato er innenfor innvilgelsesperioden`() {
        sakMedInnvilgelse(1 til 31.januar(2024))
            .erLøpende(fastClock(LocalDate.of(2024, 1, 15))) shouldBe true
    }

    @Test
    fun `erLoepende er true paa siste dag av innvilgelsesperioden`() {
        sakMedInnvilgelse(1 til 31.januar(2024))
            .erLøpende(fastClock(LocalDate.of(2024, 1, 31))) shouldBe true
    }

    @Test
    fun `erLoepende er true naar innvilgelsesperioden er i fremtiden`() {
        sakMedInnvilgelse(1 til 31.januar(2024))
            .erLøpende(fastClock(LocalDate.of(2023, 12, 31))) shouldBe true
    }

    @Test
    fun `erLoepende er false naar innvilgelsesperioden er i fortiden`() {
        sakMedInnvilgelse(1 til 31.januar(2024))
            .erLøpende(fastClock(LocalDate.of(2024, 2, 1))) shouldBe false
    }
    // endregion

    // region harÅpenSøknad
    @Test
    fun `harAapenSoeknad er false uten behandlinger`() {
        SakMother.sak().harÅpenSøknad shouldBe false
    }

    @Test
    fun `harAapenSoeknad er true for soeknadsbehandling som ikke er iverksatt og ikke avbrutt`() {
        val sak = SakMother.sak(
            behandlinger = listOf(
                BehandlingMother.tiltakspengerBehandling(
                    behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
                    behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.UNDER_BEHANDLING,
                    iverksattTidspunkt = null,
                ),
            ),
        )
        sak.harÅpenSøknad shouldBe true
    }

    @Test
    fun `harAapenSoeknad er false naar soeknadsbehandlingen er iverksatt`() {
        val sak = SakMother.sak(
            behandlinger = listOf(
                BehandlingMother.tiltakspengerBehandling(
                    behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
                    iverksattTidspunkt = LocalDateTime.parse("2024-01-15T12:00:00"),
                ),
            ),
        )
        sak.harÅpenSøknad shouldBe false
    }

    @Test
    fun `harAapenSoeknad er false naar soeknadsbehandlingen er avbrutt`() {
        val sak = SakMother.sak(
            behandlinger = listOf(
                BehandlingMother.tiltakspengerBehandling(
                    behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
                    behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.AVBRUTT,
                    iverksattTidspunkt = null,
                ),
            ),
        )
        sak.harÅpenSøknad shouldBe false
    }

    @Test
    fun `harAapenSoeknad er false for revurdering selv om den ikke er iverksatt`() {
        val sak = SakMother.sak(
            behandlinger = listOf(
                BehandlingMother.tiltakspengerBehandling(
                    behandlingstype = TiltakspengerBehandling.Behandlingstype.REVURDERING,
                    iverksattTidspunkt = null,
                ),
            ),
        )
        sak.harÅpenSøknad shouldBe false
    }
    // endregion

    // region status
    @Test
    fun `status er Avsluttet naar saken ikke har vedtak eller behandlinger`() {
        SakMother.sak().status(Clock.systemUTC()) shouldBe Saksstatus.Avsluttet
    }

    @Test
    fun `status er TilBehandling naar det er en aapen soeknadsbehandling og ingen loepende vedtak`() {
        val sak = SakMother.sak(
            behandlinger = listOf(
                BehandlingMother.tiltakspengerBehandling(
                    behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
                    iverksattTidspunkt = null,
                ),
            ),
        )
        sak.status(Clock.systemUTC()) shouldBe Saksstatus.TilBehandling
    }

    @Test
    fun `status er Loepende trumfer aapen soeknad`() {
        val sak = SakMother.sak(
            rammevedtak = listOf(
                VedtakMother.tiltakspengerVedtak(
                    sakId = "sakId",
                    virkningsperiode = 1 til 31.januar(2024),
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                ),
            ),
            behandlinger = listOf(
                BehandlingMother.tiltakspengerBehandling(
                    behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
                    iverksattTidspunkt = null,
                ),
            ),
        )
        sak.status(fastClock(LocalDate.of(2024, 1, 15))) shouldBe Saksstatus.Løpende
    }

    @Test
    fun `status er Avsluttet naar siste vedtak er avslag og ingen aapen soeknad`() {
        val sak = SakMother.sak(
            rammevedtak = listOf(
                VedtakMother.tiltakspengerVedtak(
                    sakId = "sakId",
                    rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                    opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
                ),
            ),
        )
        sak.status(fastClock(LocalDate.of(2024, 6, 1))) shouldBe Saksstatus.Avsluttet
    }

    @Test
    fun `status er Avsluttet naar siste vedtak er stans og ingen aapen soeknad`() {
        val sak = SakMother.sak(
            rammevedtak = listOf(
                VedtakMother.tiltakspengerVedtak(
                    vedtakId = "v1",
                    sakId = "sakId",
                    virkningsperiode = 1 til 31.januar(2024),
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
                ),
                VedtakMother.tiltakspengerVedtak(
                    vedtakId = "v2",
                    sakId = "sakId",
                    virkningsperiode = 15 til 31.januar(2024),
                    rettighet = TiltakspengerVedtak.Rettighet.STANS,
                    opprettetTidspunkt = LocalDateTime.parse("2024-01-15T00:00:00"),
                ),
            ),
        )
        sak.status(fastClock(LocalDate.of(2024, 2, 1))) shouldBe Saksstatus.Avsluttet
    }

    @Test
    fun `status er Avsluttet naar innvilgelse er utloept og ingen aapen soeknad`() {
        val sak = SakMother.sak(
            rammevedtak = listOf(
                VedtakMother.tiltakspengerVedtak(
                    sakId = "sakId",
                    virkningsperiode = 1 til 31.januar(2024),
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                    opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
                ),
            ),
        )
        sak.status(fastClock(LocalDate.of(2024, 2, 1))) shouldBe Saksstatus.Avsluttet
    }
    // endregion

    private fun sakMedInnvilgelse(periode: Periode) =
        SakMother.sak(
            id = "sakId",
            rammevedtak = listOf(
                VedtakMother.tiltakspengerVedtak(
                    sakId = "sakId",
                    virkningsperiode = periode,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                ),
            ),
        )
}
