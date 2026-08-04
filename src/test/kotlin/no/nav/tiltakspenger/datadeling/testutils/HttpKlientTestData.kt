package no.nav.tiltakspenger.datadeling.testutils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.Tidsgrenser
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private fun httpKlientMetadata(statusCode: Int?) = HttpKlientMetadata(
    method = "POST",
    uri = URI.create("https://example.test/endepunkt"),
    uriSynlighet = UriSynlighet.VanligLogg,
    tidsgrenser = Tidsgrenser(svar = 30.seconds, oppkobling = 10.seconds),
    rawRequestString = "{}",
    rawResponseString = "{}",
    requestHeaders = emptyMap(),
    responseHeaders = emptyMap(),
    statusCode = statusCode,
    attempts = 1,
    attemptDurations = emptyList(),
    totalDuration = Duration.ZERO,
    tidsstempler = HttpKlientTidsstempler.INGEN,
)

/** Suksess-svar slik fakes/mocks kan oppfylle [no.nav.tiltakspenger.datadeling.arena.ArenaClient]-kontrakten uten reelle HTTP-detaljer. */
fun <T> suksessRespons(body: T): Either<HttpKlientError, HttpKlientResponse<T>> =
    HttpKlientResponse(statusCode = 200, body = body, metadata = httpKlientMetadata(200)).right()

/** Feil-svar til tester som øver feilgrenen (route-500-mapping o.l.). */
fun uventetStatusFeil(statusCode: Int = 500, body: String = "arena-feil"): Either<HttpKlientError, Nothing> =
    HttpKlientError.UventetStatus(statusCode = statusCode, body = body, metadata = httpKlientMetadata(statusCode)).left()
