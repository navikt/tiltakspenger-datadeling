package no.nav.tiltakspenger.datadeling.fakes

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikkDetaljer
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.testutils.suksessRespons
import no.nav.tiltakspenger.datadeling.testutils.uventetStatusFeil
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.periode.Periode

/**
 * @param feiler Når denne er satt, svarer alle metodene med [no.nav.tiltakspenger.datadeling.testutils.uventetStatusFeil] i stedet for data.
 * Brukes av testene som øver 500-grenen i routene.
 */
class ArenaFakeClient(
    var vedtak: List<ArenaVedtak> = emptyList(),
    var meldekort: List<ArenaMeldekort> = emptyList(),
    var utbetalingshistorikk: List<ArenaUtbetalingshistorikk> = emptyList(),
    var utbetalingshistorikkDetaljer: ArenaUtbetalingshistorikkDetaljer? = null,
    var feiler: Boolean = false,
) : ArenaClient {
    override suspend fun hentVedtak(
        fnr: Fnr,
        periode: Periode,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaVedtak>>> = svar(vedtak)

    override suspend fun hentMeldekort(
        req: ArenaClient.ArenaForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaMeldekort>>> = svar(meldekort)

    override suspend fun hentUtbetalingshistorikk(
        req: ArenaClient.ArenaForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<List<ArenaUtbetalingshistorikk>>> = svar(utbetalingshistorikk)

    /** Siste forespørselen [hentUtbetalingshistorikkDetaljer] ble kalt med, så tester kan sjekke hvordan route-en tolket query-parameterne. */
    var sisteDetaljerForespørsel: ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel? = null
        private set

    override suspend fun hentUtbetalingshistorikkDetaljer(
        req: ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel,
    ): Either<HttpKlientError, HttpKlientResponse<ArenaUtbetalingshistorikkDetaljer>> {
        sisteDetaljerForespørsel = req
        return svar(utbetalingshistorikkDetaljer ?: throw IllegalStateException("utbetalingshistorikkDetaljer is not set in fake"))
    }

    private fun <T> svar(body: T): Either<HttpKlientError, HttpKlientResponse<T>> =
        if (feiler) uventetStatusFeil() else suksessRespons(body)
}
