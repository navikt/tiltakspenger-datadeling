package no.nav.tiltakspenger.datadeling.felles.app.exception

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.uri
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError

object ExceptionHandler {
    val logger = KotlinLogging.logger {}
    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ) {
        val uri = call.request.uri
        logger.error(cause) { "Ktor mottok exception i ytterste lag. Uri: $uri." }
        call.respond500InternalServerError(
            ErrorJson(
                "Noe gikk galt på serversiden",
                "server_feil",
            ),
        )
    }
}
