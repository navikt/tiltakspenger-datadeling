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
    ) {
        /** [ident] er PII og skal ikke bli med om noen logger hele objektet. Samme maskering som [Fnr]. */
        override fun toString() = "ArenaForespørsel(ident=***********, fom=$fom, tom=$tom)"

        /** Til sikkerlogg, der identen skal med. */
        fun tilSikkerlogg() = "ArenaForespørsel(ident=$ident, fom=$fom, tom=$tom)"
    }

    data class ArenaUtbetalingshistorikkDetaljerForespørsel(
        val vedtakId: Long?,
        val meldekortId: Long?,
    )
}
