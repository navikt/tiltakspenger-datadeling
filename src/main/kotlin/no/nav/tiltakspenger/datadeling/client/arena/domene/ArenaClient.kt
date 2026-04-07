package no.nav.tiltakspenger.datadeling.client.arena.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

interface ArenaClient {
    suspend fun hentVedtak(fnr: Fnr, periode: Periode): List<ArenaVedtak>
    suspend fun hentPerioder(fnr: Fnr, periode: Periode): List<PeriodisertKilde>
    suspend fun hentMeldekort(req: ArenaRequestDTO): List<ArenaMeldekort>
    suspend fun hentUtbetalingshistorikk(req: ArenaRequestDTO): List<ArenaUtbetalingshistorikk>
    suspend fun hentUtbetalingshistorikkDetaljer(req: ArenaUtbetalingshistorikkDetaljerRequest): ArenaUtbetalingshistorikkDetaljer

    data class ArenaRequestDTO(
        val ident: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class ArenaUtbetalingshistorikkDetaljerRequest(
        val vedtakId: Long?,
        val meldekortId: Long?,
    )
}
