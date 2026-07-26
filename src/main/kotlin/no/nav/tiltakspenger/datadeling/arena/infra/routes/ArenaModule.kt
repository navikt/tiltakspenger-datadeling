package no.nav.tiltakspenger.datadeling.arena.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import no.nav.tiltakspenger.datadeling.infra.ApplicationContext
import no.nav.tiltakspenger.libs.texas.IdentityProvider

/**
 * Endepunktene som leser data rett fra Arena, path-prefiks `/arena`.
 * Eier auth-provider [IdentityProvider.AZUREAD] og krever rollen `les-meldekort`.
 *
 * Dette er tiltakspenger slik det så ut før saksbehandlingen ble flyttet ut av Arena.
 * Ingenting her ligger i vår egen database — hvert kall går videre til Arena.
 */
fun Routing.arenaModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.AZUREAD.value) {
        hentArenaMeldekortRoute(applicationContext.hentArenaMeldekortService)
        hentArenaUtbetalingshistorikkRoute(applicationContext.hentArenaUtbetalingshistorikkService)
        hentArenaUtbetalingshistorikkDetaljerRoute(applicationContext.hentArenaUtbetalingshistorikkDetaljerService)
    }
}
