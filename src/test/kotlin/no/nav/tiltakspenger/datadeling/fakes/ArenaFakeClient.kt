package no.nav.tiltakspenger.datadeling.fakes

import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaClient
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaUtbetalingshistorikkDetaljer
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaVedtak
import no.nav.tiltakspenger.datadeling.client.arena.domene.PeriodisertKilde
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
    override suspend fun hentMeldekort(req: ArenaClient.ArenaRequestDTO): List<ArenaMeldekort> = meldekort
    override suspend fun hentUtbetalingshistorikk(req: ArenaClient.ArenaRequestDTO) = utbetalingshistorikk

    override suspend fun hentUtbetalingshistorikkDetaljer(
        req: ArenaClient.ArenaUtbetalingshistorikkDetaljerRequest,
    ) = utbetalingshistorikkDetaljer ?: throw IllegalStateException("utbetalingshistorikkDetaljer is not set in fake")
}
