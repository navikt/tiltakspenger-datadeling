package no.nav.tiltakspenger.datadeling.vedtak.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.vedtak.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.infra.kafka.OboYtelserKafkaProducer
import java.time.LocalDateTime

class SendTilOboService(
    private val vedtakRepo: VedtakRepo,
    private val oboYtelserKafkaProducer: OboYtelserKafkaProducer,
) {
    val log = KotlinLogging.logger { }

    fun send() {
        val vedtak = vedtakRepo.hentRammevedtakSomSkalDelesMedObo()
        if (vedtak.isNotEmpty()) {
            log.info { "Fant ${vedtak.size} rammevedtak som skal deles med OBO" }
            vedtak.forEach {
                oboYtelserKafkaProducer.sendTilObo(it.sak.fnr, it.vedtak.vedtakId)
                vedtakRepo.markerSendtTilObo(it.vedtak.vedtakId, LocalDateTime.now())
                log.info { "Markert vedtak med vedtakId ${it.vedtak.vedtakId} som delt med OBO" }
            }
        }
    }
}
