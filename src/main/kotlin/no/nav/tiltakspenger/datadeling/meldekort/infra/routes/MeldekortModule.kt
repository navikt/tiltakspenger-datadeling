package no.nav.tiltakspenger.datadeling.meldekort.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import no.nav.tiltakspenger.datadeling.infra.ApplicationContext
import no.nav.tiltakspenger.libs.texas.IdentityProvider

/**
 * Endepunktene for meldeperioder og meldekort som er behandlet hos oss.
 * Eier auth-provider [IdentityProvider.AZUREAD].
 *
 * Å lese krever rollen `les-meldekort`, å skrive krever `lagre-tiltakspenger-hendelser` — se den enkelte route.
 */
fun Routing.meldekortModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.AZUREAD.value) {
        hentMeldekortDetaljerRoute(applicationContext.hentMeldekortService)
        mottaGodkjentMeldekortbehandlingRoute(applicationContext.godkjentMeldekortbehandlingRepo)
        mottaMeldeperioderRoute(applicationContext.meldeperiodeRepo)
    }
}
