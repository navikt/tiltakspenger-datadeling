package no.nav.tiltakspenger.datadeling.identhendelse.infra
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseService
import no.nav.tiltakspenger.datadeling.infra.Configuration
import no.nav.tiltakspenger.datadeling.infra.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import tools.jackson.module.kotlin.readValue
import java.util.UUID

class IdenthendelseConsumer(
    private val identhendelseService: IdenthendelseService,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "earliest") else LocalKafkaConfig(),
) : Consumer<UUID, String> {
    private val log = KotlinLogging.logger { }

    private val consumer = ManagedKafkaConsumer(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String) {
        log.info { "Mottatt identhendelse med key $key" }
        val dto: IdenthendelseDto = objectMapper.readValue(value)
        identhendelseService.behandleIdenthendelse(
            id = key,
            gammeltFnr = Fnr.fromString(dto.gammeltFnr),
            nyttFnr = Fnr.fromString(dto.nyttFnr),
        )
    }

    override fun run() = consumer.run()
}
