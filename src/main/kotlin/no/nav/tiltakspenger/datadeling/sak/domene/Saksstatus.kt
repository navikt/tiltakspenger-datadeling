package no.nav.tiltakspenger.datadeling.sak.domene

enum class Saksstatus {
    /**
     * Dersom saken har en dag som gir rett i dag eller i fremtiden. Trumfer alle andre statuser.
     */
    Løpende,

    /**
     * Dersom saken ikke er [Løpende] og det er en søknad under behandling
     * (søknadsbehandling som ikke er iverksatt og ikke er avbrutt).
     */
    TilBehandling,

    /**
     * Saken er ikke [Løpende] og det er ingen søknad under behandling. Dekker blant annet:
     * - Bruker har søkt, men har fått avslag (siste vedtak er avslag).
     * - Siste vedtak er et stansvedtak.
     * - Siste vedtak er et opphørsvedtak.
     * - Saken har utløpt naturlig og vi har ikke andre vedtak eller søknader etter siste innvilgelse.
     * - Bruker har ingen vedtak eller søknader (defensiv – Sak finnes normalt kun etter mottatt søknad).
     */
    Avsluttet,
}
