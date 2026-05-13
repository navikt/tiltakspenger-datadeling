package no.nav.tiltakspenger.datadeling.sak.domene

import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling.Behandlingsstatus
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling.Behandlingstype
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.vedtak.domene.tilInnvilgetRammevedtakstidslinje
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class Sak(
    val id: String,
    val fnr: Fnr,
    val saksnummer: String,
    val opprettet: LocalDateTime,
    val rammevedtak: List<TiltakspengerVedtak> = emptyList(),
    val behandlinger: List<TiltakspengerBehandling> = emptyList(),
) {
    init {
        require(rammevedtak.all { it.sakId == id }) {
            "Alle rammevedtak må tilhøre sak med id $id, men fant: " +
                rammevedtak.filter { it.sakId != id }.map { it.vedtakId to it.sakId }
        }
        require(behandlinger.all { it.sakId == id }) {
            "Alle behandlinger må tilhøre sak med id $id, men fant: " +
                behandlinger.filter { it.sakId != id }.map { it.behandlingId to it.sakId }
        }
    }

    /**
     * Saken har en åpen søknad dersom det finnes en søknadsbehandling som ikke er iverksatt
     * og ikke er avbrutt. Avbrutte søknadsbehandlinger regnes ikke som åpne.
     */
    val harÅpenSøknad: Boolean
        get() = behandlinger.any {
            it.behandlingstype == Behandlingstype.SOKNADSBEHANDLING &&
                it.iverksattTidspunkt == null &&
                it.behandlingStatus != Behandlingsstatus.AVBRUTT
        }

    /**
     * Se [tilInnvilgetRammevedtakstidslinje] for detaljer.
     */
    fun innvilgetRammevedtakstidslinje(): Periodisering<TiltakspengerVedtak> =
        rammevedtak.tilInnvilgetRammevedtakstidslinje()

    /**
     * Saken er løpende dersom den innvilgede rammevedtakstidslinjen har en periode
     * som strekker seg til dagens dato eller senere.
     */
    fun erLøpende(clock: Clock): Boolean {
        val iDag = LocalDate.now(clock)
        return innvilgetRammevedtakstidslinje().perioder.any { !it.tilOgMed.isBefore(iDag) }
    }

    /**
     * Utleder [Saksstatus] basert på rammevedtakene og behandlingene på saken.
     * Se [Saksstatus] for presedensen mellom de ulike statusene.
     */
    fun status(clock: Clock): Saksstatus {
        if (erLøpende(clock)) return Saksstatus.Løpende
        if (harÅpenSøknad) return Saksstatus.TilBehandling
        return Saksstatus.Avsluttet
    }
}
