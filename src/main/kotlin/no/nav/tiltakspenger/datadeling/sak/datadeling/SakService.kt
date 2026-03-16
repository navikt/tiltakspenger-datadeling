package no.nav.tiltakspenger.datadeling.sak.datadeling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.sak.db.SakRepo
import no.nav.tiltakspenger.datadeling.sak.dto.SakDTO
import no.nav.tiltakspenger.datadeling.sak.dto.toSakDTO
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

class SakService(
    private val sakRepo: SakRepo,
    private val arenaClient: ArenaClient,
) {
    val logger = KotlinLogging.logger {}

    /**
     * Henter sak for en bruker basert på fnr.
     * Søker først i TPSAK, og hvis ikke funnet, søker i Arena.
     */
    suspend fun hentSak(fnr: Fnr): SakDTO? {
        val sakFraTpsak = sakRepo.hentForFnr(fnr)
        if (sakFraTpsak != null) {
            logger.debug { "Fant sak i TPSAK for fnr" }
            return sakFraTpsak.toSakDTO()
        }

        logger.debug { "Fant ingen sak i TPSAK, søker i Arena" }
        val vedtakFraArena = arenaClient.hentVedtak(
            fnr,
            Periode(LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31)),
        )

        return vedtakFraArena
            .sortedByDescending { it.beslutningsdato }
            .firstOrNull()
            ?.sak
            ?.toSakDTO()
    }
}
