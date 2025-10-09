package no.nav.tiltakspenger.datadeling.application.db

fun prefixColumn(alias: String?): (label: String) -> String {
    val prefix = alias?.let { "$alias." } ?: ""

    return { label: String -> prefix + label }
}
