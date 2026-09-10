package no.nav.tiltakspenger.datadeling.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.ServerReady
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.datadeling.testutils.TexasClientFake
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Bakgrunnsprosessoppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Readiness
import no.nav.tiltakspenger.libs.ktor.common.oppstart.konfigurerOppstart
import org.junit.jupiter.api.Test

/**
 * Oppstarten slik datadeling kobler den sammen: appens eget Ktor-oppsett, den ekte jobblista og den ekte consumeren.
 * Den generiske orkestreringen er testet i ktor-common, så her dekkes bare wiringen dette repoet eier.
 */
class ApplicationTest {
    private val log = KotlinLogging.logger { }

    /**
     * Verifiserer at registeret jobben og consumeren skriver målingene sine til, er det samme registeret `/metrics` skraper.
     * Det er hele poenget med at [ApplicationContext] eier registeret: sender vi inn et annet register i `Jobboppsett` eller i consumeren, forsvinner seriene stille, og varselreglene «Jobb har stoppet» og «Meldingsleser har stoppet» får aldri data.
     *
     * Consumeren konstrueres, men startes ikke.
     * Meldingsleser-målingene registreres i konstruktøren til `ManagedKafkaConsumer`, mens `run()` ville krevd en ekte Kafka-broker.
     * Jobbmålingene registreres når skedulereren starter, altså ved [ServerReady].
     */
    @Test
    fun `jobben og consumeren fører målingene sine i registeret metrics skraper`() = testApplication {
        val clock = TikkendeKlokke()
        val context = TestApplicationContextMedInMemoryDb(clock = clock, texasClient = TexasClientFake(clock))
        val readiness = Readiness()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context, readiness = readiness, visSwagger = false)
            konfigurerOppstart(
                log = log,
                isNais = false,
                readiness = readiness,
                oppsett = Bakgrunnsprosessoppsett(
                    // isNais = true gir den ekte jobblista; isNais = false videre inn gjør leader election lokal, så electorPath leses aldri.
                    // Jobben send-til-obo rekker ofte én kjøring før testen stopper appen, og går da mot produsentfaken i testkonteksten.
                    jobber = bakgrunnsprosessoppsett(applicationContext = context, isNais = true).jobber,
                    // Consumerne startes ikke her; det ville krevd en ekte broker.
                    kafkaConsumers = emptyList(),
                ),
            )
        }

        // `application { }` er lat i testoppsettet, så appen må startes eksplisitt før `app` er satt.
        startApplication()

        // Konstruerer consumeren uten å starte den, slik at meldingsleser-målingene registreres på kontekstens register.
        context.identhendelseConsumer

        app.monitor.raise(ServerReady, app.environment)

        client.get("/metrics").apply {
            status shouldBe HttpStatusCode.OK
            val metrikker = bodyAsText()
            metrikker shouldContain
                """tpts_bakgrunnsprosess_intervall_sekunder{prosess="send-til-obo",type="jobb"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_sist_vellykket_tidspunkt_sekunder{prosess="send-til-obo",type="jobb"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_intervall_sekunder{prosess="tpts.identhendelse-v1",type="meldingsleser"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_sist_vellykket_tidspunkt_sekunder{prosess="tpts.identhendelse-v1",type="meldingsleser"}"""
            // Ktor-metrikkene ligger i det samme registeret, som bevis på at det er Ktor-oppsettets register vi skraper.
            metrikker shouldContain "ktor_http_server_requests"
        }

        app.monitor.raise(ApplicationStopping, app)
    }
}
