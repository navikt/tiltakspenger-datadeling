package no.nav.tiltakspenger.datadeling.behandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

interface BehandlingRepo {
    fun lagre(behandling: TiltakspengerBehandling)

    /** Filtrerer bort behandlinger uten periode*/
    fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<TiltakspengerBehandling>
    fun hentApneBehandlinger(fnr: Fnr): List<TiltakspengeBehandlingMedSak>
    fun hentForFnr(fnr: Fnr): List<TiltakspengeBehandlingMedSak>
}
