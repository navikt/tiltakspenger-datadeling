package no.nav.tiltakspenger.datadeling.felles.app.exception

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import no.nav.tiltakspenger.datadeling.felles.app.exception.egendefinerteFeil.KallTilVedtakFeilException

object ExceptionHandler {

    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ) {
        when (cause) {
            is KallTilVedtakFeilException -> {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ExceptionResponse(cause.message ?: cause.toString()),
                )
            }
        }
    }
}
