@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.datadeling.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekort
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class FakeGodkjentMeldekortRepo : GodkjentMeldekortRepo {
    private val meldekort = Atomic(mutableMapOf<String, GodkjentMeldekort>())

    override fun lagre(meldekort: GodkjentMeldekort) {
        this.meldekort.get()[meldekort.meldekortbehandlingId.toString()] = meldekort
    }

    override fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<GodkjentMeldekort> = emptyList()

    fun alle(): List<GodkjentMeldekort> = meldekort.get().values.toList()
}
