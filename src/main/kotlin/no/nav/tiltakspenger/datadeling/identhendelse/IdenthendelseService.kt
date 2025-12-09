package no.nav.tiltakspenger.datadeling.identhendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.sak.db.SakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import java.util.UUID

class IdenthendelseService(
    private val sakRepo: SakRepo,
) {
    private val log = KotlinLogging.logger { }

    fun behandleIdenthendelse(id: UUID, identhendelseDto: IdenthendelseDto) {
        val gammeltFnr = Fnr.fromString(identhendelseDto.gammeltFnr)
        val nyttFnr = Fnr.fromString(identhendelseDto.nyttFnr)
        sakRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        log.info { "Oppdatert fnr for identhendelse med id $id" }
    }
}
