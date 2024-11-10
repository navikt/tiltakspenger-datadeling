package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.auth.systembrukerMapper
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.test.core.JwkFakeProvider
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.auth.test.core.tokenServiceForTest

class TestApplicationContext {
    val jwtGenerator = JwtGenerator()

    val tokenService: TokenService = tokenServiceForTest(
        jwkProvider = JwkFakeProvider(jwtGenerator.jwk),
        systembrukerMapper = ::systembrukerMapper,
    )
}
