package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.domene.Behandling
import java.time.LocalDate

interface BehandlingService {
    suspend fun hentBehandlinger(ident: String, fom: LocalDate, tom: LocalDate): List<Behandling>
}
