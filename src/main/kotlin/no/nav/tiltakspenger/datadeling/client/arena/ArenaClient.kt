package no.nav.tiltakspenger.datadeling.client.arena

import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

interface ArenaClient {
    suspend fun hentVedtak(fnr: Fnr, periode: Periode): List<Vedtak>
    suspend fun hentPerioder(fnr: Fnr, periode: Periode): List<PeriodisertKilde>
}
