package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.client.tp.TpClient
import no.nav.tiltakspenger.datadeling.domene.Behandling
import java.time.LocalDate

class BehandlingService(
    private val tpClient: TpClient,
) {
    suspend fun hentBehandlinger(ident: String, fom: LocalDate, tom: LocalDate): List<Behandling> {
        return tpClient.hentBehandlinger(ident, fom, tom)
    }
}
