package no.nav.tiltakspenger.datadeling

/**
 * Ferdig setning som legges på slutten av vanlige logglinjer som har en tilhørende sikkerlogg-linje.
 * Lenken går til appens logger i Google Cloud Console for riktig miljø, der sikkerloggen (team-logs) kan leses.
 */
// TODO: På sikt skal service-laget i hovedsak stå for loggingen, og da bør sikkerlogg deles i interface (domene) + impl (infra), slik at implementasjonen kan gjenbruke [no.nav.tiltakspenger.datadeling.infra.Configuration.applicationProfile].
//  Inntil videre dupliserer vi miljøvalget derfra her (NAIS_CLUSTER_NAME fra env eller system-property, der "prod-gcp" betyr prod), siden domenepakka ikke kan importere infra (jf. konsist-testene).
val SE_SIKKERLOGG: String by lazy {
    val prosjekt = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "prod-gcp" -> "tpts-prod-b5ff"
        else -> "tpts-dev-6211"
    }
    "Se sikkerlogg for mer kontekst: " +
        "https://console.cloud.google.com/logs/query;query=resource.labels.container_name%3D%22tiltakspenger-datadeling%22?project=$prosjekt"
}
