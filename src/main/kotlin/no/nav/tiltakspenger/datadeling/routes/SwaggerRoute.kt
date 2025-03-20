package no.nav.tiltakspenger.datadeling.routes

import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

fun Route.swaggerRoute() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
    swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
}
