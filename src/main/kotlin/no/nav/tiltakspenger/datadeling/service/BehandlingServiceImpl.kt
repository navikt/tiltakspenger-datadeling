package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.client.tp.TpClient
import no.nav.tiltakspenger.datadeling.domene.Behandling
import java.time.LocalDate

class BehandlingServiceImpl(
    private val tpClient: TpClient,
) : BehandlingService {
    override suspend fun hentBehandlinger(ident: String, fom: LocalDate, tom: LocalDate): List<Behandling> {
        val behandlinger = tpClient.hentBehandlinger(ident, fom, tom)

        return behandlinger
    }
}
