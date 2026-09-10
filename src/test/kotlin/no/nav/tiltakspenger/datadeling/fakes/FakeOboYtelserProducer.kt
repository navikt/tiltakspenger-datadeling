package no.nav.tiltakspenger.datadeling.fakes

import no.nav.tiltakspenger.datadeling.vedtak.OboYtelserProducer
import no.nav.tiltakspenger.libs.common.Fnr
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Fake for [OboYtelserProducer] som husker det som er sendt i stedet for å skrive til Kafka.
 * Testkonteksten bruker den slik at jobben `send-til-obo` kan kjøre uten en ekte produsent og uten Kafka-miljøvariablene.
 * Jobben kjører på skedulererens egen tråd mens testen leser fra sin, så lista er en [CopyOnWriteArrayList].
 */
class FakeOboYtelserProducer : OboYtelserProducer {
    private val sendt = CopyOnWriteArrayList<Pair<Fnr, String>>()

    override fun sendTilObo(fnr: Fnr, vedtakId: String) {
        sendt.add(fnr to vedtakId)
    }

    /** Fødselsnummer og vedtakId for hvert kall, i den rekkefølgen de kom. */
    fun alle(): List<Pair<Fnr, String>> = sendt.toList()
}
