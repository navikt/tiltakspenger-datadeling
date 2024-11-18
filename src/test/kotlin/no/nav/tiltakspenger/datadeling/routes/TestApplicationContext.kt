package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.auth.systembrukerMapper
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.test.core.JwkFakeProvider
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.auth.test.core.tokenServiceForTest
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller

class TestApplicationContext {
    val jwtGenerator = JwtGenerator()

    @Suppress("UNCHECKED_CAST")
    val tokenService: TokenService = tokenServiceForTest(
        jwkProvider = JwkFakeProvider(jwtGenerator.jwk),
        systembrukerMapper = ::systembrukerMapper as (String, String, Set<String>) -> no.nav.tiltakspenger.libs.common.GenerellSystembruker<GenerellSystembrukerrolle, GenerellSystembrukerroller<GenerellSystembrukerrolle>>,
    )
}
