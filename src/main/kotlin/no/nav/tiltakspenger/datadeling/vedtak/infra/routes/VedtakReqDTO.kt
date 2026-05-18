package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import java.time.LocalDate

data class MappingError(
    val feilmelding: String,
)

data class VedtakRequest(
    val ident: Fnr,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class VedtakReqDTO(
    val ident: String,
    val fom: String?,
    val tom: String?,
) {
    fun toVedtakRequest(): Either<MappingError, VedtakRequest> {
        // Går veien via Fnr for å bruke felles validering av ident
        val ident = try {
            Fnr.fromString(ident)
        } catch (_: Exception) {
            return MappingError(
                feilmelding = "Ident $ident er ugyldig. Må bestå av 11 siffer",
            ).left()
        }

        val fraDato = if (fom.isNullOrBlank()) {
            LocalDate.of(1970, 1, 1)
        } else {
            try {
                LocalDate.parse(fom)
            } catch (_: Exception) {
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
            } catch (_: Exception) {
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

    fun toSakRequest(): Either<MappingError, Fnr> {
        return try {
            Fnr.fromString(ident).right()
        } catch (_: Exception) {
            MappingError(
                feilmelding = "Ident $ident er ugyldig. Må bestå av 11 siffer",
            ).left()
        }
    }
}
