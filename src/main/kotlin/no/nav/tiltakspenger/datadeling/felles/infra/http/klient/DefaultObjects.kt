package no.nav.tiltakspenger.datadeling.felles.infra.http.klient

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import java.time.Duration

private val LOG = KotlinLogging.logger {}

private const val SIXTY_SECONDS = 60L
fun httpClientCIO(timeout: Long = SIXTY_SECONDS) = HttpClient(CIO).config(timeout)
fun httpClientGeneric(engine: HttpClientEngine, timeout: Long = SIXTY_SECONDS) = HttpClient(engine).config(timeout)
fun httpClientWithRetry(timeout: Long = SIXTY_SECONDS) = httpClientCIO(timeout).also { httpClient ->
    httpClient.config {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            retryOnException(maxRetries = 3, retryOnTimeout = true)
            constantDelay(100, 0, false)
        }
    }
}

private fun HttpClient.config(timeout: Long) = this.config {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(HttpTimeout) {
        connectTimeoutMillis = Duration.ofSeconds(timeout).toMillis()
        requestTimeoutMillis = Duration.ofSeconds(timeout).toMillis()
        socketTimeoutMillis = Duration.ofSeconds(timeout).toMillis()
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                LOG.info { "HttpClient detaljer logget til securelog" }
                sikkerlogg.info { message }
            }
        }
        level = LogLevel.INFO
    }
    expectSuccess = true
}
