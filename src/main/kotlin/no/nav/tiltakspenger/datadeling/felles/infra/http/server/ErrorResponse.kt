package no.nav.tiltakspenger.datadeling.felles.infra.http.server

import io.ktor.http.HttpStatusCode

data class ErrorResponse(
    val json: ErrorJson,
    val httpStatus: HttpStatusCode,
)
