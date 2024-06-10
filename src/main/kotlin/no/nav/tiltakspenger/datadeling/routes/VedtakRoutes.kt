package no.nav.tiltakspenger.datadeling.routes

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.service.VedtakService
import java.time.LocalDate

private val LOG = KotlinLogging.logger {}

internal const val vedtakPath = "/vedtak"

fun Route.vedtakRoutes(
    vedtakService: VedtakService,
) {
    post("$vedtakPath/hent") {
        LOG.info { "Mottatt kall på hent vedtak" }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                { call.respond(HttpStatusCode.BadRequest, it.feilmelding) },
                {
                    val vedtak = vedtakService.hentVedtak(
                        ident = it.ident,
                        fom = it.fom,
                        tom = it.tom,
                    )
                    call.respond(status = HttpStatusCode.OK, vedtak)
                },
            )
    }
}

data class MappingError(
    val feilmelding: String,
)

data class VedtakRequest(
    val ident: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class VedtakReqDTO(
    val ident: String?,
    val fom: String?,
    val tom: String?,
) {
    fun toVedtakRequest(): Either<MappingError, VedtakRequest> {
        if (ident.isNullOrBlank()) {
            return MappingError(
                feilmelding = "Mangler ident",
            ).left()
        }

        val fraDato = if (fom.isNullOrBlank()) {
            LocalDate.of(1970, 1, 1)
        } else {
            try {
                LocalDate.parse(fom)
            } catch (e: Exception) {
                return MappingError(
                    feilmelding = "Ugyldig datoformat for fom-dato: $fom",
                ).left()
            }
        }

        val tilDato = if (tom.isNullOrBlank()) {
            LocalDate.of(9999, 12, 31)
        } else {
            try {
                LocalDate.parse(tom)
            } catch (e: Exception) {
                return MappingError(
                    feilmelding = "Ugyldig datoformat for tom-dato: $tom",
                ).left()
            }
        }

        if (fraDato.isAfter(tilDato)) {
            return MappingError(
                feilmelding = "Fra-dato $fraDato ikke være etter til-dato $tilDato",
            ).left()
        }

        return VedtakRequest(
            ident = ident,
            fom = fraDato,
            tom = tilDato,
        ).right()
    }
}
