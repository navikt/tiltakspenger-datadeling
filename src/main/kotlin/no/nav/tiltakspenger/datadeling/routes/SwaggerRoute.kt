package no.nav.tiltakspenger.datadeling.routes

import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.routing.Route

fun Route.swaggerRoute() {
    openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
}
