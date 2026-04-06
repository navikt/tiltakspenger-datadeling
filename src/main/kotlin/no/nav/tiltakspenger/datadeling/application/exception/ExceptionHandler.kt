package no.nav.tiltakspenger.datadeling.application.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.uri
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import tools.jackson.core.exc.StreamReadException
import tools.jackson.databind.DatabindException
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.exc.UnrecognizedPropertyException
import tools.jackson.module.kotlin.KotlinInvalidNullException

object ExceptionHandler {
    val logger = KotlinLogging.logger {}
    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ) {
        val uri = call.request.uri

        cause.toClientInputError()?.let { errorJson ->
            logger.debug(cause) {
                "Returnerer 400 Bad Request for klientfeil. Uri: $uri. Feil: $errorJson."
            }
            call.respond400BadRequest(errorJson)
            return
        }

        logger.error(cause) { "Returnerer 500 Internal Server Error for ukjent feil. Trekk ut feiltypen til egen catch og egen logger/custom respons. Uri: $uri." }
        call.respond500InternalServerError(
            ErrorJson(
                "Noe gikk galt på serversiden",
                "server_feil",
            ),
        )
    }
}

private fun Throwable.toClientInputError(): ErrorJson? {
    val causeChain = generateSequence(this) { it.cause }.toList()
    causeChain.filterIsInstance<KotlinInvalidNullException>().firstOrNull()?.let {
        val fieldPath = it.propertyName
        return ErrorJson(
            melding = "Mangler påkrevd felt '$fieldPath'.",
            kode = "mangler_påkrevd_felt",
        )
    }

    causeChain.filterIsInstance<UnrecognizedPropertyException>().firstOrNull()?.let {
        val fieldPath = it.propertyName ?: it.fieldPath()
        return ErrorJson(
            melding = "Ugyldig forespørsel. Ukjent felt '$fieldPath'.",
            kode = "ukjent_felt",
        )
    }

    causeChain.filterIsInstance<InvalidFormatException>().firstOrNull()?.let {
        val fieldPath = it.fieldPath()
        return ErrorJson(
            melding = "Ugyldig verdi i felt '$fieldPath'. Kontroller datatype og format.",
            kode = "ugyldig_verdi",
        )
    }

    causeChain.filterIsInstance<MismatchedInputException>().firstOrNull()?.let {
        val fieldPath = it.fieldPath()
        return ErrorJson(
            melding = "Felt '$fieldPath' har feil datatype eller struktur.",
            kode = "feil_datatype_eller_struktur",
        )
    }

    if (causeChain.any { it is ContentTransformationException || it is BadRequestException || it is StreamReadException }) {
        return ErrorJson(
            melding = "Ugyldig JSON i forespørselen. Kontroller syntaksen.",
            kode = "ugyldig_json",
        )
    }

    return null
}

private fun DatabindException.fieldPath(): String {
    return path
        .mapNotNull {
            when {
                !it.propertyName.isNullOrBlank() -> it.propertyName
                it.index >= 0 -> "[${it.index}]"
                else -> null
            }
        }
        .joinToString(separator = ".")
        .ifBlank { "request-body" }
}
