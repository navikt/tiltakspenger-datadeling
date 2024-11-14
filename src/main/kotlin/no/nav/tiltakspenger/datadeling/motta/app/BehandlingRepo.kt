package no.nav.tiltakspenger.datadeling.motta.app

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

interface BehandlingRepo {
    fun lagre(behandling: TiltakspengerBehandling)
    fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
        kilde: String,
    ): List<TiltakspengerBehandling>
}
