package no.nav.tiltakspenger.datadeling.routes

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.datadeling.behandling.motta.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.behandling.motta.routes.mottaNyBehandlingRoute
import no.nav.tiltakspenger.datadeling.meldekort.db.GodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.meldekort.db.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.meldekort.motta.routes.mottaGodkjentMeldekortRoute
import no.nav.tiltakspenger.datadeling.meldekort.motta.routes.mottaMeldeperioderRoute
import no.nav.tiltakspenger.datadeling.vedtak.motta.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.motta.routes.mottaNyttVedtakRoute
import java.time.Clock

fun Route.mottaRoutes(
    mottaNyttVedtakService: MottaNyttVedtakService,
    mottaNyBehanlingService: MottaNyBehandlingService,
    clock: Clock,
    meldeperiodeRepo: MeldeperiodeRepo,
    godkjentMeldekortRepo: GodkjentMeldekortRepo,
) {
    this.mottaNyttVedtakRoute(mottaNyttVedtakService, clock)
    this.mottaNyBehandlingRoute(mottaNyBehanlingService, clock)
    this.mottaMeldeperioderRoute(meldeperiodeRepo)
    this.mottaGodkjentMeldekortRoute(godkjentMeldekortRepo)
}
