package no.nav.tiltakspenger.datadeling.testutils

import java.time.Clock
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator

class TestApplicationContext(
    clock: Clock,
) {
    val jwtGenerator = JwtGenerator()
    val texasClient = TexasClientFake(clock)
}
