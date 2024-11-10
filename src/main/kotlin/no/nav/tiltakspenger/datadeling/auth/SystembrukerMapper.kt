package no.nav.tiltakspenger.datadeling.auth

import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller

private val logger = KotlinLogging.logger { }

internal fun systembrukerMapper(
    brukernavn: String,
    roller: Set<String>,
): Systembruker {
    return Systembruker(
        brukernavn = brukernavn,
        roller = Systembrukerroller(
            roller.mapNotNull {
                when (it) {
                    "lagre-tiltakspenger-hendelser" -> Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER
                    "les-vedtak" -> Systembrukerrolle.LES_VEDTAK
                    "les-behandling" -> Systembrukerrolle.LES_BEHANDLING
                    else -> null.also {
                        logger.debug { "Filtrerer bort ukjent systembruker: $it" }
                    }
                }
            }.toSet(),
        ),
    )
}
