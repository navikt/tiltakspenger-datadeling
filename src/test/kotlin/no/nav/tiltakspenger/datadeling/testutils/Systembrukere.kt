package no.nav.tiltakspenger.datadeling.testutils

import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.Systembrukerroller
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator

/**
 * Rollenavnet slik det står i `roles`-claimet i Entra ID-tokenet.
 * Speiler mappingen i `no.nav.tiltakspenger.datadeling.infra.auth.systembrukerMapper`.
 */
fun Systembrukerrolle.tokenrolle(): String = when (this) {
    Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER -> "lagre-tiltakspenger-hendelser"
    Systembrukerrolle.LES_VEDTAK -> "les-vedtak"
    Systembrukerrolle.LES_BEHANDLING -> "les-behandling"
    Systembrukerrolle.LES_MELDEKORT -> "les-meldekort"
}

/**
 * Registrerer en systembruker med [roller] i texas-faken, og returnerer et token som autentiserer som den.
 */
fun TestApplicationContext.leggTilSystembruker(
    vararg roller: Systembrukerrolle,
    klientnavn: String = "klientnavn",
): String = leggTilSystembruker(jwtGenerator, texasClient, roller.toList(), klientnavn)

/**
 * Registrerer en systembruker med [roller] i texas-faken, og returnerer et token som autentiserer som den.
 */
fun TestApplicationContextMedInMemoryDb.leggTilSystembruker(
    vararg roller: Systembrukerrolle,
    klientnavn: String = "klientnavn",
): String = leggTilSystembruker(jwtGenerator, texasClient, roller.toList(), klientnavn)

private fun leggTilSystembruker(
    jwtGenerator: JwtGenerator,
    texasClient: TexasClientFake,
    roller: List<Systembrukerrolle>,
    klientnavn: String,
): String {
    val token = jwtGenerator.createJwtForSystembruker(roles = roller.map { it.tokenrolle() })
    texasClient.leggTilSystembruker(
        token,
        Systembruker(
            roller = Systembrukerroller(roller),
            klientnavn = klientnavn,
            klientId = "id",
        ),
    )
    return token
}
