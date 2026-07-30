package no.nav.tiltakspenger.datadeling.infra.routes
import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.swaggerRoute() {
    // CORS er en route-scoped plugin, og mottakeren her er rot-routingen.
    // Installerte vi den rett på mottakeren, ville anyHost() gjelde alle appens endepunkter og ikke bare specen.
    // Derfor egen route-node rundt, med swaggerUI på tom sti slik at URL-ene fortsatt er /swagger og /swagger/documentation.yaml.
    route("swagger") {
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
        }
        swaggerUI(path = "", swaggerFile = "openapi/documentation.yaml")
    }
}
