package no.nav.tiltakspenger.datadeling.routes

import no.nav.tiltakspenger.datadeling.felles.TexasClientFake
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator

class TestApplicationContext {
    val jwtGenerator = JwtGenerator()
    val texasClient = TexasClientFake()
}
