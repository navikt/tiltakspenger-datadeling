@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.datadeling.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.datadeling.behandling.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.TiltakspengeBehandlingMedSak
import no.nav.tiltakspenger.datadeling.behandling.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.behandling.apneBehandlingsstatuser
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class FakeBehandlingRepo : BehandlingRepo {
    private val behandlinger = Atomic(mutableMapOf<String, TiltakspengerBehandling>())

    override fun lagre(behandling: TiltakspengerBehandling) {
        behandlinger.get()[behandling.behandlingId] = behandling
    }

    override fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<TiltakspengerBehandling> = emptyList()

    override fun hentApneBehandlinger(fnr: Fnr): List<TiltakspengeBehandlingMedSak> = emptyList()

    override fun hentForFnr(fnr: Fnr): List<TiltakspengeBehandlingMedSak> = emptyList()

    fun alle(): List<TiltakspengerBehandling> = behandlinger.get().values.toList()
}
