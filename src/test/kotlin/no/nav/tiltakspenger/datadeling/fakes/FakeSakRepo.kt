package no.nav.tiltakspenger.datadeling.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.datadeling.sak.db.SakRepo
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId

class FakeSakRepo : SakRepo {
    private val saker = Atomic(mutableMapOf<SakId, Sak>())

    override fun lagre(sak: Sak) {
        saker.get()[sak.id] = sak
    }

    override fun hentForId(id: SakId): Sak? {
        return saker.get()[id]
    }

    override fun hentForFnr(fnr: Fnr): Sak? {
        return saker.get().values.find { it.fnr == fnr }
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        val entry = saker.get().entries.find { it.value.fnr == gammeltFnr } ?: return
        saker.get()[entry.key] = entry.value.copy(fnr = nyttFnr)
    }

    fun alle(): List<Sak> = saker.get().values.toList()
}
