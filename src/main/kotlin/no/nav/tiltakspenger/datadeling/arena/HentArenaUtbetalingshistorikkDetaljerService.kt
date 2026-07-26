package no.nav.tiltakspenger.datadeling.arena

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.httpklient.loggSuksess
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

/**
 * Henter detaljene Arena har om én utbetaling, slått opp på meldekort- eller vedtak-id.
 * Serverer `GET /arena/utbetalingshistorikk/detaljer`.
 */
class HentArenaUtbetalingshistorikkDetaljerService(
    private val arenaClient: ArenaClient,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentArenaUtbetalingshistorikkDetaljer(
        meldekortId: Long?,
        vedtakId: Long?,
    ): Either<HttpKlientError, ArenaUtbetalingshistorikkDetaljer> {
        return arenaClient.hentUtbetalingshistorikkDetaljer(
            ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel(
                meldekortId = meldekortId,
                vedtakId = vedtakId,
            ),
        )
            .onLeft { it.loggFeil(logger, "henting av utbetalingshistorikkdetaljer fra Arena", "vedtakId: $vedtakId, meldekortId: $meldekortId", sikkerlogg) }
            .onRight { it.loggSuksess(logger, "Hentet utbetalingshistorikkdetaljer fra Arena.", sikkerlogg) }
            .map { it.body }
    }
}
