package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.vedtak.VedtakClient
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

class VedtakServiceImpl(
    private val vedtakClient: VedtakClient,
    private val arenaClient: ArenaClient,
) : VedtakService {
    override suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak> {
//        val vedtak = vedtakClient.hent(ident, fom, tom)
        val arena = arenaClient.hentVedtak(ident, fom, tom)

        return arena
    }

    override suspend fun hentPerioder(ident: String, fom: LocalDate, tom: LocalDate): List<Periode> {
//        val vedtak = vedtakClient.hent(ident, fom, tom)
        val arena = arenaClient.hentPerioder(ident, fom, tom)

        return arena
    }
}
