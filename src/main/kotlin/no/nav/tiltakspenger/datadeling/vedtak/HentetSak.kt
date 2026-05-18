package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.TILTAKSPENGER
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
import java.time.Clock
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

fun SakForVedtakSak.toHentetSak(clock: Clock) = HentetSak(
    sakId = id.toString(),
    saksnummer = saksnummer.verdi,
    kilde = "TPSAK",
    status = status(clock).name,
    opprettetDato = opprettet,
    iverksattSoknadsbehandlingTidspunkt = rammevedtak.iverksattSøknadsbehandlingTidspunkt(),
)

private fun List<TiltakspengerVedtak>.iverksattSøknadsbehandlingTidspunkt(): LocalDateTime? =
    sortedBy { it.opprettet }
        .firstOrNull { it.rettighet in listOf(TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG) }
        ?.opprettet

fun ArenaVedtak.Sak.toHentetSak() = HentetSak(
    sakId = sakId,
    saksnummer = saksnummer,
    kilde = "ARENA",
    status = status,
    opprettetDato = opprettetDato.atTime(9, 0),
    iverksattSoknadsbehandlingTidspunkt = null,
)
