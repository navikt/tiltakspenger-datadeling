package no.nav.tiltakspenger.datadeling.behandling.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import no.nav.tiltakspenger.datadeling.infra.ApplicationContext
import no.nav.tiltakspenger.libs.texas.IdentityProvider

/**
 * Endepunktene for behandlinger i tiltakspenger-saksbehandling-api.
 * Eier auth-provider [IdentityProvider.AZUREAD].
 *
 * Å lese krever rollen `les-behandling`, å skrive krever `lagre-tiltakspenger-hendelser` — se den enkelte route.
 */
fun Routing.behandlingModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.AZUREAD.value) {
        hentBehandlingerForPeriodeRoute(applicationContext.hentBehandlingerForPeriodeService)
        hentApneBehandlingerRoute(applicationContext.hentApneBehandlingerService)
        mottaNyBehandlingRoute(applicationContext.mottaNyBehandlingService, applicationContext.clock)
    }
}
