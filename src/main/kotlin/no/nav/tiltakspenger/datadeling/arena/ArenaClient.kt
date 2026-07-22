package no.nav.tiltakspenger.datadeling.arena

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

/**
 * Suksess-siden er pakket i [HttpKlientResponse] slik at kallende service kan logge parvis med `loggSuksess` (PII-fri linje i vanlig logg + rå request/respons i sikkerlogg) — datadeling skal ha sporbarhet på Arena-oppslag også når kallet lykkes.
 */
interface ArenaClient {
    suspend fun hentVedtak(fnr: Fnr, periode: Periode): Either<HttpKlientError, HttpKlientResponse<List<ArenaVedtak>>>
    suspend fun hentMeldekort(req: ArenaForespørsel): Either<HttpKlientError, HttpKlientResponse<List<ArenaMeldekort>>>
    suspend fun hentUtbetalingshistorikk(req: ArenaForespørsel): Either<HttpKlientError, HttpKlientResponse<List<ArenaUtbetalingshistorikk>>>
    suspend fun hentUtbetalingshistorikkDetaljer(req: ArenaUtbetalingshistorikkDetaljerForespørsel): Either<HttpKlientError, HttpKlientResponse<ArenaUtbetalingshistorikkDetaljer>>

    data class ArenaForespørsel(
        val ident: String,
        val fom: LocalDate,
        val tom: LocalDate,
    ) {
        /**
         * [ident] er PII og skal ikke bli med om noen logger hele objektet.
         * Samme maskering som [Fnr].
         */
        override fun toString() = "ArenaForespørsel(ident=***********, fom=$fom, tom=$tom)"
    }

    data class ArenaUtbetalingshistorikkDetaljerForespørsel(
        val vedtakId: Long?,
        val meldekortId: Long?,
    )
}
