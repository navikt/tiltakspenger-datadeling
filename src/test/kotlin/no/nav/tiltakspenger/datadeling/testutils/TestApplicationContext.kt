package no.nav.tiltakspenger.datadeling.testutils

import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator

class TestApplicationContext {
    val jwtGenerator = JwtGenerator()
    val texasClient = TexasClientFake()
}
