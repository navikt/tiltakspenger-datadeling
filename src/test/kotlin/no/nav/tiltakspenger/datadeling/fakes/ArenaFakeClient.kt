package no.nav.tiltakspenger.datadeling.fakes

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikkDetaljer
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.testutils.suksessRespons
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.periode.Periode

class ArenaFakeClient(
    var vedtak: List<ArenaVedtak> = emptyList(),
    var meldekort: List<ArenaMeldekort> = emptyList(),
    var utbetalingshistorikk: List<ArenaUtbetalingshistorikk> = emptyList(),
    var utbetalingshistorikkDetaljer: ArenaUtbetalingshistorikkDetaljer? = null,
) : ArenaClient {
    override suspend fun hentVedtak(
        fnr: Fnr,
        periode: Periode,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaVedtak>>> = suksessRespons(vedtak)

    override suspend fun hentMeldekort(
        req: ArenaClient.ArenaForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaMeldekort>>> = suksessRespons(meldekort)

    override suspend fun hentUtbetalingshistorikk(
        req: ArenaClient.ArenaForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaUtbetalingshistorikk>>> = suksessRespons(utbetalingshistorikk)

    override suspend fun hentUtbetalingshistorikkDetaljer(
        req: ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<ArenaUtbetalingshistorikkDetaljer>> =
        suksessRespons(utbetalingshistorikkDetaljer ?: throw IllegalStateException("utbetalingshistorikkDetaljer is not set in fake"))
}
