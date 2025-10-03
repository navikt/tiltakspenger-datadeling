package no.nav.tiltakspenger.datadeling.application.db

import no.nav.tiltakspenger.libs.json.objectMapper
import org.postgresql.util.PGobject

fun toPGObject(value: Any?) = PGobject().also {
    it.type = "json"
    it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
