package no.nav.tiltakspenger.datadeling.identhendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.meldekort.db.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import java.util.UUID

class IdenthendelseService(
    private val behandlingRepo: BehandlingRepo,
    private val vedtakRepo: VedtakRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
) {
    private val log = KotlinLogging.logger { }

    fun behandleIdenthendelse(id: UUID, identhendelseDto: IdenthendelseDto) {
        val gammeltFnr = Fnr.fromString(identhendelseDto.gammeltFnr)
        val nyttFnr = Fnr.fromString(identhendelseDto.nyttFnr)
        behandlingRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        vedtakRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        meldeperiodeRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        log.info { "Oppdatert fnr for identhendelse med id $id" }
    }
}
