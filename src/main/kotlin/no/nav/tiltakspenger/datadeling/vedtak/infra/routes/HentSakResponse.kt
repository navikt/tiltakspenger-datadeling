package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import no.nav.tiltakspenger.datadeling.vedtak.HentetSak
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

fun HentetSak.toHentSakResponse() = HentSakResponse(
    sakId = sakId,
    saksnummer = saksnummer,
    kilde = kilde,
    status = status,
    opprettetDato = opprettetDato,
    iverksattSoknadsbehandlingTidspunkt = iverksattSoknadsbehandlingTidspunkt,
)
