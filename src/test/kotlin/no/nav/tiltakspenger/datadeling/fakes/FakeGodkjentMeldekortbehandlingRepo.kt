@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.datadeling.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandling
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandlingRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class FakeGodkjentMeldekortbehandlingRepo : GodkjentMeldekortbehandlingRepo {
    private val meldekort = Atomic(mutableMapOf<String, GodkjentMeldekortbehandling>())

    override fun lagre(meldekort: GodkjentMeldekortbehandling) {
        this.meldekort.get()[meldekort.meldekortbehandlingId.toString()] = meldekort
    }

    override fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<GodkjentMeldekortbehandling> = emptyList()

    fun alle(): List<GodkjentMeldekortbehandling> = meldekort.get().values.toList()
}
