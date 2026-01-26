package no.nav.tiltakspenger.datadeling.utbetalingshistorikk

import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes.ArenaUtbetalingshistorikkResponse
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

class ArenaUtbetalingshistorikkService(
    private val arenaClient: ArenaClient,
) {
    suspend fun hentUtbetalingshistorikk(fnr: Fnr, periode: Periode): List<ArenaUtbetalingshistorikkResponse> {
        return arenaClient.hentUtbetalingshistorikk(
            ArenaClient.ArenaRequestDTO(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        ).map {
            ArenaUtbetalingshistorikkResponse(
                meldekortId = it.meldekortId,
                dato = it.dato,
                transaksjonstype = it.transaksjonstype,
                sats = it.sats,
                status = it.status,
                vedtakId = it.vedtakId,
                belop = it.belop,
                fraOgMedDato = it.fraOgMedDato,
                tilOgMedDato = it.tilOgMedDato,
            )
        }
    }
}
