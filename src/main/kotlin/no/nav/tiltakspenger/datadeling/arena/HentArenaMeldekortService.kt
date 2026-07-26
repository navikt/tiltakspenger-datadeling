package no.nav.tiltakspenger.datadeling.arena

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.httpklient.loggSuksess
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Henter meldekortene Arena har for en person i en periode.
 * Serverer `POST /arena/meldekort`.
 */
class HentArenaMeldekortService(
    private val arenaClient: ArenaClient,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentArenaMeldekort(fnr: Fnr, periode: Periode): Either<HttpKlientError, List<ArenaMeldekort>> {
        return arenaClient.hentMeldekort(
            ArenaClient.ArenaForespørsel(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        )
            .onLeft { it.loggFeil(logger, "henting av meldekort fra Arena", "Periode: $periode", sikkerlogg) }
            .onRight { it.loggSuksess(logger, "Hentet meldekort fra Arena.", sikkerlogg) }
            .map { it.body }
    }
}
