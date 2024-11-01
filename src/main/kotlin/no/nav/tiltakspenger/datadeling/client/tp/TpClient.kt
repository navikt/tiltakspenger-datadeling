package no.nav.tiltakspenger.datadeling.client.tp

import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

interface TpClient {
    suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak>
    suspend fun hentVedtakPerioder(ident: String, fom: LocalDate, tom: LocalDate): List<PeriodisertKilde>
    suspend fun hentBehandlinger(ident: String, fom: LocalDate, tom: LocalDate): List<Behandling>
}
