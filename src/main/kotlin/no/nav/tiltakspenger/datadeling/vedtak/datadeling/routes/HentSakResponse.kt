package no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes

import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaVedtak
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengeSakMedVedtak
import java.time.Clock
import java.time.LocalDateTime

/**
 * Response-DTO brukt kun av `POST /vedtak/sak` (saas-proxy).
 *
 * Skiller seg fra [no.nav.tiltakspenger.datadeling.sak.dto.SakDTO] ved at den inneholder
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

fun TiltakspengeSakMedVedtak.toHentSakResponse(clock: Clock) = HentSakResponse(
    sakId = sak.id,
    saksnummer = sak.saksnummer,
    kilde = "TPSAK",
    // Vi populerer rammevedtak på Sak for å kunne utlede status. Behandlinger er ikke tilgjengelig
    // her, så harÅpenSøknad-baserte statuser kan ikke utledes via denne pathen ennå.
    status = sak.copy(rammevedtak = vedtak).status(clock).name,
    opprettetDato = sak.opprettet,
    iverksattSoknadsbehandlingTidspunkt = iverksattSøknadsbehandlingTidspunkt,
)

fun Sak.toHentSakResponse(clock: Clock) = HentSakResponse(
    sakId = id,
    saksnummer = saksnummer,
    kilde = "TPSAK",
    status = status(clock).name,
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
