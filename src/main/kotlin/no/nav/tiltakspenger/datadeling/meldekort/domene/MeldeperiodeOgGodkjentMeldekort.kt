package no.nav.tiltakspenger.datadeling.meldekort.domene

data class MeldeperiodeOgGodkjentMeldekort(
    val meldeperiode: Meldeperiode,
    val godkjentMeldekort: GodkjentMeldekort?,
)
