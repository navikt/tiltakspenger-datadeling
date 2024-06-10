package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

interface VedtakService {
    suspend fun hentVedtak(ident: String, fom: LocalDate, tom: LocalDate): List<Vedtak>
}
