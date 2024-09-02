package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.client.vedtak.VedtakClient
import no.nav.tiltakspenger.datadeling.domene.Behandling
import java.time.LocalDate

class BehandlingServiceImpl(
    private val vedtakClient: VedtakClient,
) : BehandlingService {
    override suspend fun hentBehandlinger(ident: String, fom: LocalDate, tom: LocalDate): List<Behandling> {
        val behandlinger = vedtakClient.hentBehandlinger(ident, fom, tom)

        return behandlinger
    }
}
