package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.tp.TpClient
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

class VedtakService(
    private val tpClient: TpClient,
    private val arenaClient: ArenaClient,
) {
    suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak> {
        val vedtak = tpClient.hentVedtak(ident, fom, tom)
        val arena = arenaClient.hentVedtak(ident, fom, tom)

        return arena + vedtak
    }

    suspend fun hentPerioder(ident: String, fom: LocalDate, tom: LocalDate): List<PeriodisertKilde> {
        val vedtak = tpClient.hentVedtakPerioder(ident, fom, tom)
        val arena = arenaClient.hentPerioder(ident, fom, tom)

        return arena + vedtak
    }
}
