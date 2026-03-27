package no.nav.tiltakspenger.datadeling.application

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.MappingError

private val logger = KotlinLogging.logger {}

internal suspend inline fun ApplicationCall.withOptionalVedtakId(
    paramName: String = "vedtakId",
    crossinline onSuccess: suspend (Long?) -> Unit,
) {
    withOptionalValidQueryParam(
        paramName = paramName,
        parse = String::toLong,
        errorMessage = "Ugyldig query-parameter 'vedtakId'. Må være et heltall.",
        onSuccess = onSuccess,
    )
}

internal suspend inline fun ApplicationCall.withOptionalMeldekortId(
    paramName: String = "meldekortId",
    crossinline onSuccess: suspend (Long?) -> Unit,
) {
    withOptionalValidQueryParam(
        paramName = paramName,
        parse = String::toLong,
        errorMessage = "Ugyldig query-parameter 'meldekortId'. Må være et heltall.",
        onSuccess = onSuccess,
    )
}

internal suspend inline fun <T> ApplicationCall.withOptionalValidQueryParam(
    paramName: String,
    parse: (String) -> T,
    errorMessage: String,
    crossinline onSuccess: suspend (T?) -> Unit,
) {
    val value = request.queryParameters[paramName]
    if (value.isNullOrBlank()) {
        onSuccess(null)
        return
    }

    Either.catch {
        parse(value)
    }.fold(
        ifLeft = {
            val error = MappingError(errorMessage)
            logger.debug { "Feil ved parsing av query-parameter '$paramName'. Underliggende feil: $error" }
            respond(HttpStatusCode.BadRequest, error)
        },
        ifRight = { onSuccess(it) },
    )
}
