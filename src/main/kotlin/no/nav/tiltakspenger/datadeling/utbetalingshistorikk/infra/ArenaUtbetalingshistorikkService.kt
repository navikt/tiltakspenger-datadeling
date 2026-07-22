package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikkDetaljer
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.httpklient.loggSuksess
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periode.Periode

class ArenaUtbetalingshistorikkService(
    private val arenaClient: ArenaClient,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentUtbetalingshistorikk(
        fnr: Fnr,
        periode: Periode,
    ): Either<HttpKlientError, List<ArenaUtbetalingshistorikk>> {
        return arenaClient.hentUtbetalingshistorikk(
            ArenaClient.ArenaForespørsel(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        )
            .onLeft { it.loggFeil(logger, "henting av utbetalingshistorikk fra Arena", "Periode: $periode", sikkerlogg) }
            .onRight { it.loggSuksess(logger, "Hentet utbetalingshistorikk fra Arena.", sikkerlogg) }
            .map { it.body }
    }

    suspend fun hentUtbetalingshistorikkDetaljer(
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
