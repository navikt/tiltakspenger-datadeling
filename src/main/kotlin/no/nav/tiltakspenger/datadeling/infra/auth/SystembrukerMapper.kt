package no.nav.tiltakspenger.datadeling.infra.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.Systembrukerroller

private val logger = KotlinLogging.logger { }

internal fun systembrukerMapper(
    klientId: String,
    klientnavn: String,
    roller: Set<String>,
): Systembruker {
    return Systembruker(
        roller = Systembrukerroller(
            roller.mapNotNull { rolle ->
                when (rolle) {
                    "lagre-tiltakspenger-hendelser" -> Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER

                    "les-vedtak" -> Systembrukerrolle.LES_VEDTAK

                    "les-behandling" -> Systembrukerrolle.LES_BEHANDLING

                    "les-meldekort" -> Systembrukerrolle.LES_MELDEKORT

                    "access_as_application" -> null

                    else -> null.also {
                        logger.debug { "Filtrerer bort ukjent systembrukerrolle: $rolle" }
                    }
                }
            }.toSet(),
        ),
        klientId = klientId,
        klientnavn = klientnavn,
    )
}
