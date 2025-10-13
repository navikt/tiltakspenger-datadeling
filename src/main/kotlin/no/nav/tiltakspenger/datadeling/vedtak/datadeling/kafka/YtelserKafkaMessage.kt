package no.nav.tiltakspenger.datadeling.vedtak.datadeling.kafka

data class YtelserKafkaMessage(
    val personId: String,
    val meldingstype: String = "OPPRETT",
    val ytelsestype: String = "TILTAKSPENGER",
    val kildesystem: String = "TPSAK",
)
