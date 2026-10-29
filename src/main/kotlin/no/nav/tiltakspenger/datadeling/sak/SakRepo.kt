package no.nav.tiltakspenger.datadeling.sak

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

interface SakRepo {
    fun lagre(sak: Sak)
    fun hentForId(id: SakId): Sak?
    fun hentForFnr(fnr: Fnr): Sak?
    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr)
}
