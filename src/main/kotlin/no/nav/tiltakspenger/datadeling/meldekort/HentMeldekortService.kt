package no.nav.tiltakspenger.datadeling.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.Clock

/**
 * Henter meldeperiodene og de godkjente meldekortene vi har for en person i en periode.
 * Serverer `POST /meldekort/detaljer`.
 */
class HentMeldekortService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val clock: Clock,
) {
    fun hentMeldekort(fnr: Fnr, periode: Periode): Meldekortoversikt {
        val meldeperioderOgGodkjenteMeldekort = meldeperiodeRepo.hentMeldeperioderOgGodkjenteMeldekort(fnr, periode)

        return Meldekortoversikt(
            meldeperioderKlareTilUtfylling = meldeperioderOgGodkjenteMeldekort
                .filter { it.godkjentMeldekortbehandling == null && it.meldeperiode.erKlarTilUtfylling(clock) }
                .map { it.meldeperiode }
                .sortedByDescending { it.fraOgMed },
            godkjenteMeldekort = meldeperioderOgGodkjenteMeldekort
                .mapNotNull { rad -> rad.godkjentMeldekortbehandling?.iKjede(rad.meldeperiode.kjedeId) }
                .sortedByDescending { it.meldeperiode.fraOgMed },
        )
    }

    private fun GodkjentMeldekortbehandling.iKjede(kjedeId: String) = GodkjentMeldekortIKjede(
        behandling = this,
        meldeperiode = meldeperioder.single { it.kjedeId == kjedeId },
    )
}
