package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.vedtak.VedtakClient
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

class VedtakServiceImpl(
    private val vedtakClient: VedtakClient,
    private val arenaClient: ArenaClient,
) : VedtakService {
    override suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak> {
//        val vedtak = vedtakClient.hent(ident, fom, tom)
        val arena = arenaClient.hent(ident, fom, tom)

        return arena
    }
}
