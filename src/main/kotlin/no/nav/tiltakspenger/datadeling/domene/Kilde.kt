package no.nav.tiltakspenger.datadeling.domene

/**
 * Ren domenemodell.
 * Skal ikke brukes til serialisering eller deserialisering. Skal heller ikke persisteres direkte i basen.
 */
enum class Kilde {
    TILTAKSPENGER,
    ARENA,
}
