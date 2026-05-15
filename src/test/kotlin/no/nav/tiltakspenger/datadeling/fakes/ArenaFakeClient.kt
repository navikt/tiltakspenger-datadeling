package no.nav.tiltakspenger.datadeling.fakes

import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikkDetaljer
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.arena.PeriodisertKilde
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class ArenaFakeClient(
    var vedtak: List<ArenaVedtak> = emptyList(),
    var perioder: List<PeriodisertKilde> = emptyList(),
    var meldekort: List<ArenaMeldekort> = emptyList(),
    var utbetalingshistorikk: List<ArenaUtbetalingshistorikk> = emptyList(),
    var utbetalingshistorikkDetaljer: ArenaUtbetalingshistorikkDetaljer? = null,
) : ArenaClient {
    override suspend fun hentVedtak(fnr: Fnr, periode: Periode): List<ArenaVedtak> = vedtak
    override suspend fun hentPerioder(fnr: Fnr, periode: Periode): List<PeriodisertKilde> = perioder
    override suspend fun hentMeldekort(req: ArenaClient.ArenaForespørsel): List<ArenaMeldekort> = meldekort
    override suspend fun hentUtbetalingshistorikk(req: ArenaClient.ArenaForespørsel) = utbetalingshistorikk

    override suspend fun hentUtbetalingshistorikkDetaljer(
        req: ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel,
    ) = utbetalingshistorikkDetaljer ?: throw IllegalStateException("utbetalingshistorikkDetaljer is not set in fake")
}
