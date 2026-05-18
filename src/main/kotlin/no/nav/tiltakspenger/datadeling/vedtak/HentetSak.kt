package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.sak.Sak
import java.time.LocalDateTime

/**
 * Domenemodell for saksinformasjon som returneres fra HentSakService.
 */
data class HentetSak(
    val sakId: String,
    val saksnummer: String,
    val kilde: String,
    val status: String,
    val opprettetDato: LocalDateTime,
    val iverksattSoknadsbehandlingTidspunkt: LocalDateTime?,
)

fun TiltakspengeSakMedVedtak.toHentetSak() = HentetSak(
    sakId = sak.id.toString(),
    saksnummer = sak.saksnummer.verdi,
    kilde = "TPSAK",
    status = "Løpende",
    opprettetDato = sak.opprettet,
    iverksattSoknadsbehandlingTidspunkt = iverksattSøknadsbehandlingTidspunkt,
)

fun Sak.toHentetSak() = HentetSak(
    sakId = id.toString(),
    saksnummer = saksnummer.verdi,
    kilde = "TPSAK",
    status = "Løpende",
    opprettetDato = opprettet,
    iverksattSoknadsbehandlingTidspunkt = null,
)

fun ArenaVedtak.Sak.toHentetSak() = HentetSak(
    sakId = sakId,
    saksnummer = saksnummer,
    kilde = "ARENA",
    status = status,
    opprettetDato = opprettetDato.atTime(9, 0),
    iverksattSoknadsbehandlingTidspunkt = null,
)
