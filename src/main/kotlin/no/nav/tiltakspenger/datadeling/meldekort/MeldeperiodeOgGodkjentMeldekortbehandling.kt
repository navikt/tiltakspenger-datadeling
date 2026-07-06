package no.nav.tiltakspenger.datadeling.meldekort

data class MeldeperiodeOgGodkjentMeldekortbehandling(
    val meldeperiode: Meldeperiode,
    val godkjentMeldekortbehandling: GodkjentMeldekortbehandling?,
)
