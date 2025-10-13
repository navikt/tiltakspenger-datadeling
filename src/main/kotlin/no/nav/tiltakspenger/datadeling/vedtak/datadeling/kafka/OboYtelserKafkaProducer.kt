package no.nav.tiltakspenger.datadeling.vedtak.datadeling.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.kafka.Producer

class OboYtelserKafkaProducer(
    val kafkaProducer: Producer<String, String>,
    val topic: String,
) {
    val log = KotlinLogging.logger { }

    fun sendTilObo(fnr: Fnr, vedtakId: String) {
        val melding = objectMapper.writeValueAsString(YtelserKafkaMessage(personId = fnr.verdi))
        kafkaProducer.produce(topic, fnr.verdi, melding)
        log.info { "Skrev melding til topic for vedtakId $vedtakId" }
    }
}
