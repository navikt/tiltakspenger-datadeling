package no.nav.tiltakspenger.datadeling.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

interface MeldeperiodeRepo {
    fun lagre(meldeperioder: List<Meldeperiode>)
    fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<Meldeperiode>
    fun hentMeldeperioderOgGodkjenteMeldekort(fnr: Fnr, periode: Periode): List<MeldeperiodeOgGodkjentMeldekort>
}
