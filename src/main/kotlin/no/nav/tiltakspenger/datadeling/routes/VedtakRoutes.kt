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
import no.nav.tiltakspenger.datadeling.Configuration.applicationProfile
import no.nav.tiltakspenger.datadeling.Profile
import no.nav.tiltakspenger.datadeling.domene.Periode
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.common.Fnr
import java.time.LocalDate

private val LOG = KotlinLogging.logger {}

internal const val vedtakPath = "/vedtak"

fun Route.vedtakRoutes(
    vedtakService: VedtakService,
) {
    post("$vedtakPath/detaljer") {
        LOG.info { "Mottatt kall på hent detaljer" }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                { call.respond(HttpStatusCode.BadRequest, it) },
                {
                    // Samtidighetskontroll prodsettes 02.10.24
                    // Vi har ikke noe data i prod, så vi svarer med tom liste i først omgang
                    // Trellokort med beskrivelser https://trello.com/c/5Q9Cag7x/1093-legge-til-rette-for-prodsetting-av-samtidighetskontroll-i-arena
                    if (applicationProfile() == Profile.PROD) {
                        call.respond(HttpStatusCode.OK, emptyList<Vedtak>())
                    } else if (applicationProfile() == Profile.DEV || applicationProfile() == Profile.LOCAL) {
                        try {
                            val vedtak = vedtakService.hentVedtak(
                                ident = it.ident,
                                fom = it.fom,
                                tom = it.tom,
                            )
                            call.respond(status = HttpStatusCode.OK, vedtak)
                        } catch (e: Exception) {
                            call.respond(
                                status = HttpStatusCode.InternalServerError,
                                message = InternalError(feilmelding = e.message ?: "Ukjent feil"),
                            )
                        }
                    }
                },
            )
    }

    post("$vedtakPath/perioder") {
        LOG.info { "Mottatt kall på hent perioder" }
        call.receive<VedtakReqDTO>().toVedtakRequest()
            .fold(
                { call.respond(HttpStatusCode.BadRequest, it) },
                {
                    // Samtidighetskontroll prodsettes 02.10.24
                    // Vi har ikke noe data i prod, så vi svarer med tom liste i først omgang
                    // Trellokort med beskrivelser https://trello.com/c/5Q9Cag7x/1093-legge-til-rette-for-prodsetting-av-samtidighetskontroll-i-arena
                    if (applicationProfile() == Profile.PROD) {
                        call.respond(HttpStatusCode.OK, emptyList<Periode>())
                    } else if (applicationProfile() == Profile.DEV || applicationProfile() == Profile.LOCAL) {
                        try {
                            val perioder = vedtakService.hentPerioder(
                                ident = it.ident,
                                fom = it.fom,
                                tom = it.tom,
                            )
                            call.respond(status = HttpStatusCode.OK, perioder)
                        } catch (e: Exception) {
                            call.respond(
                                status = HttpStatusCode.InternalServerError,
                                message = InternalError(feilmelding = e.message ?: "Ukjent feil"),
                            )
                        }
                    }
                },
            )
    }
}

data class InternalError(
    val feilmelding: String,
)

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

        // Går veien via Fnr for å bruke felles validering av ident
        val ident = try {
            Fnr.fromString(ident)
        } catch (e: Exception) {
            return MappingError(
                feilmelding = "Ident $ident er ugyldig. Må bestå av 11 siffer",
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
            ident = ident.verdi,
            fom = fraDato,
            tom = tilDato,
        ).right()
    }
}
