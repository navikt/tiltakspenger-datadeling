package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr

interface OboYtelserProducer {
    fun sendTilObo(fnr: Fnr, vedtakId: String)
}
