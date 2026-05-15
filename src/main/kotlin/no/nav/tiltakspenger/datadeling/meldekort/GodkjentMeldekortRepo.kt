package no.nav.tiltakspenger.datadeling.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

interface GodkjentMeldekortRepo {
    fun lagre(meldekort: GodkjentMeldekort)
    fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<GodkjentMeldekort>
}
