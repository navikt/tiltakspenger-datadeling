package no.nav.tiltakspenger.datadeling.arena

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

interface ArenaClient {
    suspend fun hentVedtak(fnr: Fnr, periode: Periode): List<ArenaVedtak>
    suspend fun hentPerioder(fnr: Fnr, periode: Periode): List<PeriodisertKilde>
    suspend fun hentMeldekort(req: ArenaForespørsel): List<ArenaMeldekort>
    suspend fun hentUtbetalingshistorikk(req: ArenaForespørsel): List<ArenaUtbetalingshistorikk>
    suspend fun hentUtbetalingshistorikkDetaljer(req: ArenaUtbetalingshistorikkDetaljerForespørsel): ArenaUtbetalingshistorikkDetaljer

    data class ArenaForespørsel(
        val ident: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class ArenaUtbetalingshistorikkDetaljerForespørsel(
        val vedtakId: Long?,
        val meldekortId: Long?,
    )
}
