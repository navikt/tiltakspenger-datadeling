package no.nav.tiltakspenger.datadeling.behandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Enkel stub som returnerer det testen setter opp.
 * Brukt av service-testene i denne pakken; rute-testene bruker [no.nav.tiltakspenger.datadeling.fakes.FakeBehandlingRepo] eller ekte database.
 */
internal class TestBehandlingRepo(
    private val behandlingerForFnrOgPeriode: List<TiltakspengerBehandling> = emptyList(),
    private val apneBehandlinger: List<TiltakspengeBehandlingMedSak> = emptyList(),
) : BehandlingRepo {
    override fun lagre(behandling: TiltakspengerBehandling) = Unit

    override fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<TiltakspengerBehandling> = behandlingerForFnrOgPeriode

    override fun hentApneBehandlinger(fnr: Fnr): List<TiltakspengeBehandlingMedSak> = apneBehandlinger

    override fun hentForFnr(fnr: Fnr): List<TiltakspengeBehandlingMedSak> = emptyList()
}
