package no.nav.tiltakspenger.datadeling.identhendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.sak.SakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import java.util.UUID

class IdenthendelseService(
    private val sakRepo: SakRepo,
) {
    private val log = KotlinLogging.logger { }

    fun behandleIdenthendelse(id: UUID, gammeltFnr: Fnr, nyttFnr: Fnr) {
        sakRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        log.info { "Oppdatert fnr for identhendelse med id $id" }
    }
}
