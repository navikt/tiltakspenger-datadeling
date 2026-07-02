package no.nav.tiltakspenger.datadeling.vedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.behandling.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SakForVedtakSakTest {
    private val clock = Clock.fixed(Instant.parse("2024-01-15T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `tom sak er avsluttet og ikke lopende`() {
        val sak = SakMother.sak().toSakForVedtakSak()

        sak.erLøpende(clock) shouldBe false
        sak.harÅpenSøknad shouldBe false
        sak.status(clock) shouldBe Saksstatus.Avsluttet
    }

    @Test
    fun `sak med innvilget rammevedtak som gjelder i dag er lopende`() {
        val sak = SakMother.sak()
        val vedtak = VedtakMother.tiltakspengerVedtak(
            sakId = sak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
            virkningsperiode = 1.januar(2024) til 31.januar(2024),
        )

        sak.toSakForVedtakSak(rammevedtak = listOf(vedtak)).status(clock) shouldBe Saksstatus.Løpende
    }

    @Test
    fun `lopende trumfer apen soknad`() {
        val sak = SakMother.sak()
        val vedtak = VedtakMother.tiltakspengerVedtak(
            sakId = sak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
            virkningsperiode = 1.januar(2024) til 31.januar(2024),
        )
        val behandling = BehandlingMother.tiltakspengerBehandling(
            sakId = sak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
            iverksattTidspunkt = null,
        )

        sak.toSakForVedtakSak(
            rammevedtak = listOf(vedtak),
            behandlinger = listOf(behandling),
        ).status(clock) shouldBe Saksstatus.Løpende
    }

    @Test
    fun `sak med apen soknadsbehandling er til behandling nar den ikke er lopende`() {
        val sak = SakMother.sak()
        val behandling = BehandlingMother.tiltakspengerBehandling(
            sakId = sak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
            iverksattTidspunkt = null,
        )

        sak.toSakForVedtakSak(behandlinger = listOf(behandling)).status(clock) shouldBe Saksstatus.TilBehandling
    }

    @Test
    fun `avbrutt soknadsbehandling regnes ikke som apen soknad`() {
        val sak = SakMother.sak()
        val behandling = BehandlingMother.tiltakspengerBehandling(
            sakId = sak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
            behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.AVBRUTT,
            iverksattTidspunkt = null,
        )

        sak.toSakForVedtakSak(behandlinger = listOf(behandling)).status(clock) shouldBe Saksstatus.Avsluttet
    }

    @Test
    fun `avslag gir avsluttet sak`() {
        val sak = SakMother.sak()
        val vedtak = VedtakMother.tiltakspengerVedtak(
            sakId = sak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
            rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
        )

        sak.toSakForVedtakSak(rammevedtak = listOf(vedtak)).status(clock) shouldBe Saksstatus.Avsluttet
    }

    @Test
    fun `sak validerer at rammevedtak og behandlinger tilhorer samme sak`() {
        val sak = SakMother.sak(id = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV")
        val annenSak = SakMother.sak(id = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAA")
        val feilVedtak = VedtakMother.tiltakspengerVedtak(sakId = annenSak.id)
        val feilBehandling = BehandlingMother.tiltakspengerBehandling(sakId = annenSak.id)

        shouldThrow<IllegalArgumentException> { sak.toSakForVedtakSak(rammevedtak = listOf(feilVedtak)) }
        shouldThrow<IllegalArgumentException> { sak.toSakForVedtakSak(behandlinger = listOf(feilBehandling)) }
    }

    private fun no.nav.tiltakspenger.datadeling.sak.Sak.toSakForVedtakSak(
        rammevedtak: List<TiltakspengerVedtak> = emptyList(),
        behandlinger: List<TiltakspengerBehandling> = emptyList(),
    ): SakForVedtakSak = SakForVedtakSak(
        id = id,
        fnr = fnr,
        saksnummer = saksnummer,
        opprettet = opprettet,
        rammevedtak = rammevedtak,
        behandlinger = behandlinger,
    )
}
