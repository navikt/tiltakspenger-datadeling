@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.datadeling.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.datadeling.meldekort.Meldeperiode
import no.nav.tiltakspenger.datadeling.meldekort.MeldeperiodeOgGodkjentMeldekort
import no.nav.tiltakspenger.datadeling.meldekort.MeldeperiodeRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class FakeMeldeperiodeRepo : MeldeperiodeRepo {
    private val meldeperioder = Atomic(mutableMapOf<String, Meldeperiode>())

    override fun lagre(meldeperioder: List<Meldeperiode>) {
        val map = this.meldeperioder.get()
        meldeperioder.forEach { map[it.id.toString()] = it }
    }

    override fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<Meldeperiode> = emptyList()

    override fun hentMeldeperioderOgGodkjenteMeldekort(fnr: Fnr, periode: Periode): List<MeldeperiodeOgGodkjentMeldekort> = emptyList()

    fun alle(): List<Meldeperiode> = meldeperioder.get().values.toList()
}
