package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import no.nav.tiltakspenger.datadeling.infra.ApplicationContext
import no.nav.tiltakspenger.libs.texas.IdentityProvider

/**
 * Endepunktene for vedtak, og for saken vedtakene hører til.
 * Eier auth-provider [IdentityProvider.AZUREAD].
 *
 * Å lese krever rollen `les-vedtak`, å skrive krever `lagre-tiltakspenger-hendelser` — se den enkelte route.
 * De fire leseendepunktene svarer på hvert sitt spørsmål om de samme vedtakene: gjeldende rett (`/detaljer`), rett over tid inkludert Arena (`/perioder`), historikken bak tidslinjen (`/tidslinje`) og saken alene (`/sak`).
 */
fun Routing.vedtakModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.AZUREAD.value) {
        hentVedtakDetaljerRoute(applicationContext.hentVedtakDetaljerService, applicationContext.clock)
        hentVedtakPerioderRoute(applicationContext.hentVedtakPerioderService)
        hentVedtakTidslinjeRoute(applicationContext.hentVedtakTidslinjeService)
        hentSakRoute(applicationContext.hentSakService)
        mottaNyttVedtakRoute(applicationContext.mottaNyttVedtakService, applicationContext.clock)
    }
}
