package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.sak.Sak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengeSakMedVedtak
import java.time.LocalDateTime

/**
 * Response-DTO brukt kun av `POST /vedtak/sak` (saas-proxy).
 *
 * Skiller seg fra [no.nav.tiltakspenger.datadeling.sak.infra.SakDTO] ved at den inneholder
 * [iverksattSoknadsbehandlingTidspunkt].
 */
data class HentSakResponse(
    val sakId: String,
    val saksnummer: String,
    val kilde: String,
    val status: String,
    val opprettetDato: LocalDateTime,
    /**
     * Tidspunktet første søknadsbehandling ble iverksatt for denne saken.
     * Kan være null dersom det ikke finnes noen iverksatt søknadsbehandling enda,
     * eller dersom kilden ikke har denne informasjonen (f.eks. Arena).
     */
    val iverksattSoknadsbehandlingTidspunkt: LocalDateTime?,
)

fun TiltakspengeSakMedVedtak.toHentSakResponse() = HentSakResponse(
    sakId = sak.id.toString(),
    saksnummer = sak.saksnummer.verdi,
    kilde = "TPSAK",
    status = "Løpende",
    opprettetDato = sak.opprettet,
    iverksattSoknadsbehandlingTidspunkt = iverksattSøknadsbehandlingTidspunkt,
)

fun Sak.toHentSakResponse() = HentSakResponse(
    sakId = id.toString(),
    saksnummer = saksnummer.verdi,
    kilde = "TPSAK",
    status = "Løpende",
    opprettetDato = opprettet,
    iverksattSoknadsbehandlingTidspunkt = null,
)

fun ArenaVedtak.Sak.toHentSakResponse() = HentSakResponse(
    sakId = sakId,
    saksnummer = saksnummer,
    kilde = "ARENA",
    status = status,
    opprettetDato = opprettetDato.atTime(9, 0),
    iverksattSoknadsbehandlingTidspunkt = null,
)
