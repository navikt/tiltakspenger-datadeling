package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.datadeling.behandling.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class SakForVedtakSak(
    val id: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val opprettet: LocalDateTime,
    val rammevedtak: List<TiltakspengerVedtak>,
    val behandlinger: List<TiltakspengerBehandling>,
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

    val harInnhold: Boolean
        get() = rammevedtak.isNotEmpty() || behandlinger.isNotEmpty()

    val harÅpenSøknad: Boolean
        get() = behandlinger.any {
            it.behandlingstype == TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING &&
                it.iverksattTidspunkt == null &&
                it.erApenBehandling()
        }

    fun innvilgetRammevedtakstidslinje(): Periodisering<TiltakspengerVedtak> =
        rammevedtak.hentInnvilgetTidslinje()

    fun erLøpende(clock: Clock): Boolean {
        val iDag = LocalDate.now(clock)
        return innvilgetRammevedtakstidslinje().perioder.any { !it.tilOgMed.isBefore(iDag) }
    }

    /**
     * Utleder sakens status etter reglene beskrevet i [Saksstatus].
     */
    fun status(clock: Clock): Saksstatus = when {
        erLøpende(clock) -> Saksstatus.Løpende
        harÅpenSøknad -> Saksstatus.TilBehandling
        else -> Saksstatus.Avsluttet
    }
}

enum class Saksstatus {
    /**
     * Dersom saken har en dag som gir rett i dag eller i fremtiden. Trumfer alle andre statuser.
     */
    Løpende,

    /**
     * Dersom saken ikke er [Løpende] og det er en søknad under behandling
     * (søknadsbehandling som ikke er iverksatt og ikke er avbrutt).
     */
    TilBehandling,

    /**
     * Saken er ikke [Løpende] og det er ingen søknad under behandling. Dekker blant annet:
     * - Bruker har søkt, men har fått avslag (siste vedtak er avslag).
     * - Siste vedtak er et stansvedtak.
     * - Siste vedtak er et opphørsvedtak.
     * - Saken har utløpt naturlig og vi har ikke andre vedtak eller søknader etter siste innvilgelse.
     * - Bruker har ingen vedtak eller søknader (defensiv – Sak finnes normalt kun etter mottatt søknad).
     */
    Avsluttet,
}
