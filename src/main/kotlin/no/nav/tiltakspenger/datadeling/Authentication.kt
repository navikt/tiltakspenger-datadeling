package no.nav.tiltakspenger.datadeling

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.authentication(config: TokenValidationConfig) {
    install(Authentication) {
        tokenValidationSupport(
            name = config.name,
            config = TokenSupportConfig(
                IssuerConfig(
                    name = config.name,
                    discoveryUrl = config.discoveryUrl,
                    acceptedAudience = config.acceptedAudience,
                ),
            ),
        )
    }
}

data class TokenValidationConfig(
    val name: String,
    val discoveryUrl: String,
    val acceptedAudience: List<String>,
)
