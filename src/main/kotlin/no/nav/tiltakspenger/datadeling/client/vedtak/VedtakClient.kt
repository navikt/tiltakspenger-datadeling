package no.nav.tiltakspenger.datadeling.client.vedtak

import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.domene.Periode
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

interface VedtakClient {
    suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak>
    suspend fun hentVedtakPerioder(ident: String, fom: LocalDate, tom: LocalDate): List<Periode>
    suspend fun hentBehandlinger(ident: String, fom: LocalDate, tom: LocalDate): List<Behandling>
}
