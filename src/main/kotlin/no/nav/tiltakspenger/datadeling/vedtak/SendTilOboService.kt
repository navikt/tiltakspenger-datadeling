package no.nav.tiltakspenger.datadeling.vedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import java.time.Clock

class SendTilOboService(
    private val vedtakRepo: VedtakRepo,
    private val oboYtelserProducer: OboYtelserProducer,
    private val clock: Clock,
) {
    val log = KotlinLogging.logger { }

    fun send() {
        val vedtak = vedtakRepo.hentRammevedtakSomSkalDelesMedObo()
        if (vedtak.isNotEmpty()) {
            log.info { "Fant ${vedtak.size} rammevedtak som skal deles med OBO" }
            vedtak.forEach {
                oboYtelserProducer.sendTilObo(it.sak.fnr, it.vedtak.vedtakId)
                vedtakRepo.markerSendtTilObo(it.vedtak.vedtakId, nå(clock))
                log.info { "Markert vedtak med vedtakId ${it.vedtak.vedtakId} som delt med OBO" }
            }
        }
    }
}
