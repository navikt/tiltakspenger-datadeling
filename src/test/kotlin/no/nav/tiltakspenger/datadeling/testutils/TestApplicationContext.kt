package no.nav.tiltakspenger.datadeling.testutils

import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import java.time.Clock

class TestApplicationContext(
    clock: Clock = TikkendeKlokke(),
) {
    val jwtGenerator = JwtGenerator()
    val texasClient = TexasClientFake(clock)
}
