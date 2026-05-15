package no.nav.tiltakspenger.datadeling.vedtak.infra.kafka

data class YtelserKafkaMessage(
    val personId: String,
    val meldingstype: String = "OPPRETT",
    val ytelsestype: String = "TILTAKSPENGER",
    val kildesystem: String = "TPSAK",
)
