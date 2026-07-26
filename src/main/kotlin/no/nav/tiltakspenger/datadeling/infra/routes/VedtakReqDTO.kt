package no.nav.tiltakspenger.datadeling.infra.routes

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

/**
 * 400-responsen for alle endepunktene, på formen `{"feilmelding": "..."}`.
 */
data class MappingError(
    val feilmelding: String,
)

/**
 * En ferdig validert request: hvem det spørres om, og for hvilken periode.
 */
data class FnrOgPeriode(
    val fnr: Fnr,
    val periode: Periode,
)

/**
 * Request-DTO-en de fleste leseendepunktene deler: `ident` med valgfri `fom`/`tom`.
 *
 * Navnet er låst til komponentskjemaet `VedtakReqDTO` i openapi-specen — konsumenter (bl.a. modiapersonoversikt) genererer klienter fra den, så et mer beskrivende `FnrOgPeriodeRequestDTO` ville brutt kontrakten deres.
 * Den ligger derfor her, felles for alle features, i stedet for i `vedtak`, som eide den før.
 */
data class VedtakReqDTO(
    val ident: String,
    val fom: String?,
    val tom: String?,
) {
    /**
     * Blank eller manglende `fom`/`tom` betyr «så langt tilbake og fram som vi har data».
     */
    fun toFnrOgPeriode(): Either<MappingError, FnrOgPeriode> {
        val fnr = toFnr().getOrElse { return it.left() }

        val fraDato = if (fom.isNullOrBlank()) {
            LocalDate.of(1970, 1, 1)
        } else {
            try {
                LocalDate.parse(fom)
            } catch (_: Exception) {
                return MappingError(
                    feilmelding = "Ugyldig datoformat i felt 'fom'. Forventet format er yyyy-MM-dd.",
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
                    feilmelding = "Ugyldig datoformat i felt 'tom'. Forventet format er yyyy-MM-dd.",
                ).left()
            }
        }

        if (fraDato.isAfter(tilDato)) {
            return MappingError(
                feilmelding = "Fra-dato kan ikke være etter til-dato.",
            ).left()
        }

        return FnrOgPeriode(
            fnr = fnr,
            periode = Periode(fraOgMed = fraDato, tilOgMed = tilDato),
        ).right()
    }

    /**
     * For endepunktene som kun slår opp på person, og ser bort fra `fom`/`tom`.
     */
    fun toFnr(): Either<MappingError, Fnr> {
        // Går veien via Fnr for å bruke felles validering av ident
        return try {
            Fnr.fromString(ident).right()
        } catch (_: Exception) {
            MappingError(
                feilmelding = "Ugyldig ident. Må bestå av 11 siffer.",
            ).left()
        }
    }
}
