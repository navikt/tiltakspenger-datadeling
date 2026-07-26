package no.nav.tiltakspenger.datadeling.meldekort

/**
 * Meldekortbildet for én person i én periode.
 *
 * @param meldeperioderKlareTilUtfylling Meldeperioder brukeren kan fylle ut nå, men ikke har sendt inn.
 * Nyeste periode først.
 * @param godkjenteMeldekort Meldekort som er ferdigbehandlet og vedtatt, ett per meldeperiodekjede.
 * Nyeste periode først.
 */
data class Meldekortoversikt(
    val meldeperioderKlareTilUtfylling: List<Meldeperiode>,
    val godkjenteMeldekort: List<GodkjentMeldekortIKjede>,
)

/**
 * En godkjent meldekortbehandling sett fra én meldeperiodekjede.
 *
 * En behandling kan dekke flere kjeder, så den alene sier ikke hvilken periode svaret gjelder.
 * [meldeperiode] er behandlingens meldeperiode for den kjeden oppslaget kom fra.
 */
data class GodkjentMeldekortIKjede(
    val behandling: GodkjentMeldekortbehandling,
    val meldeperiode: GodkjentMeldekortbehandling.Meldeperiode,
)
