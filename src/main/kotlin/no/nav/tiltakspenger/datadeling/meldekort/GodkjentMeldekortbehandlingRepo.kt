package no.nav.tiltakspenger.datadeling.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

interface GodkjentMeldekortbehandlingRepo {
    fun lagre(meldekort: GodkjentMeldekortbehandling)
    fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<GodkjentMeldekortbehandling>
}
