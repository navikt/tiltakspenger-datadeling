package no.nav.tiltakspenger.datadeling.infra

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.datadeling.testutils.withTestApplicationContextInMemory
import org.junit.jupiter.api.Test

/**
 * Oppsettet som binder alt sammen: helsesjekker, swagger og feature-modulene.
 */
class KtorSetupTest {

    @Test
    fun `helsesjekkene svarer uten autentisering`() {
        withTestApplicationContextInMemory {
            client.get("/isalive").status shouldBe HttpStatusCode.OK
            // Readiness starter som ikke-klar; appen melder seg klar først når oppstarten er ferdig.
            client.get("/isready").status shouldBe HttpStatusCode.ServiceUnavailable
        }
    }

    @Test
    fun `swagger er tilgjengelig nar den er skrudd pa`() {
        withTestApplicationContextInMemory(visSwagger = true) {
            client.get("/swagger").status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `swagger er ikke tilgjengelig nar den er skrudd av`() {
        withTestApplicationContextInMemory(visSwagger = false) {
            client.get("/swagger").status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `endepunktene krever token`() {
        withTestApplicationContextInMemory {
            client.post("/vedtak/sak").status shouldBe HttpStatusCode.Unauthorized
        }
    }
}
