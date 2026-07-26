package no.nav.tiltakspenger.datadeling.infra.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.RoutingContext
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.texas.systembruker

private val logger = KotlinLogging.logger { }

/**
 * Autentiserer systembrukeren bak kallet og sjekker at den har [rolle], før [block] kjøres.
 *
 * Alle endepunktene i denne appen kalles av systembrukere over Entra ID og gjør den samme kontrollen først.
 * Ved å samle kontrollen her inneholder route-filene kun det som er unikt for endepunktet.
 *
 * Svarer selv med 403 og kode `mangler_rolle` når rollen mangler, og kaller da ikke [block].
 * Feil i selve token-mappingen svares på av `call.systembruker(...)` i libs.
 */
internal suspend fun RoutingContext.medSystembruker(
    rolle: Systembrukerrolle,
    block: suspend (Systembruker) -> Unit,
) {
    val endepunkt = "${call.request.httpMethod.value} ${call.request.path()}"
    val systembruker = call.systembruker(systemBrukerMapper()) as? Systembruker ?: return

    if (!systembruker.roller.harRolle(rolle)) {
        logger.warn { "Systembruker ${systembruker.klientnavn} fikk 403 Forbidden mot $endepunkt. Underliggende feil: Mangler rollen $rolle" }
        call.respond403Forbidden(
            "Mangler rollen $rolle. Har rollene: ${systembruker.roller.toList()}",
            "mangler_rolle",
        )
        return
    }

    logger.debug { "Systembruker ${systembruker.klientnavn} kaller $endepunkt" }
    block(systembruker)
}

/**
 * Mapper et verifisert systembruker-token til [Systembruker].
 */
@Suppress("UNCHECKED_CAST")
private fun systemBrukerMapper() = ::systembrukerMapper as (String, String, Set<String>) -> GenerellSystembruker<
    GenerellSystembrukerrolle,
    GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    >
