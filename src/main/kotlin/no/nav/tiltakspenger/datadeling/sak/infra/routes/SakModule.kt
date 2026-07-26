package no.nav.tiltakspenger.datadeling.sak.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import no.nav.tiltakspenger.datadeling.infra.ApplicationContext
import no.nav.tiltakspenger.libs.texas.IdentityProvider

/**
 * Endepunktene for saker fra tiltakspenger-saksbehandling-api.
 * Eier auth-provider [IdentityProvider.AZUREAD] og krever rollen `lagre-tiltakspenger-hendelser`.
 *
 * Saken er inngangen til alt annet vi lagrer: både vedtak og behandlinger avvises hvis saken ikke finnes fra før.
 */
fun Routing.sakModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.AZUREAD.value) {
        mottaSakRoute(applicationContext.mottaSakService)
    }
}
