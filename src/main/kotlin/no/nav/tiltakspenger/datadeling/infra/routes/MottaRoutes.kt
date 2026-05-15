package no.nav.tiltakspenger.datadeling.infra.routes
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.behandling.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.behandling.infra.routes.mottaNyBehandlingRoute
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.meldekort.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.meldekort.infra.routes.mottaGodkjentMeldekortRoute
import no.nav.tiltakspenger.datadeling.meldekort.infra.routes.mottaMeldeperioderRoute
import no.nav.tiltakspenger.datadeling.sak.SakRepo
import no.nav.tiltakspenger.datadeling.sak.infra.routes.mottaSakRoute
import no.nav.tiltakspenger.datadeling.vedtak.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.mottaNyttVedtakRoute
import java.time.Clock

fun Route.mottaRoutes(
    mottaNyttVedtakService: MottaNyttVedtakService,
    mottaNyBehanlingService: MottaNyBehandlingService,
    clock: Clock,
    meldeperiodeRepo: MeldeperiodeRepo,
    godkjentMeldekortRepo: GodkjentMeldekortRepo,
    sakRepo: SakRepo,
) {
    this.mottaNyttVedtakRoute(mottaNyttVedtakService, clock)
    this.mottaNyBehandlingRoute(mottaNyBehanlingService, clock)
    this.mottaMeldeperioderRoute(meldeperiodeRepo)
    this.mottaGodkjentMeldekortRoute(godkjentMeldekortRepo)
    this.mottaSakRoute(sakRepo)
}
