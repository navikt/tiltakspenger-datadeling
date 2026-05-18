package no.nav.tiltakspenger.datadeling.vedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.sak.SakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

class HentSakService(
    private val vedtakRepo: VedtakRepo,
    private val sakRepo: SakRepo,
    private val arenaClient: ArenaClient,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Henter sak for en bruker basert på fnr.
     * Søker først i TPSAK, og hvis ikke funnet, søker i Arena.
     */
    suspend fun hentSak(fnr: Fnr): HentetSak? {
        val sakMedVedtakFraTpsak = vedtakRepo.hentSakMedVedtakForFnr(fnr)
        if (sakMedVedtakFraTpsak != null) {
            logger.debug {
                "Fant sak med vedtak i TPSAK for fnr. sakId=${sakMedVedtakFraTpsak.sak.id}, saksnummer=${sakMedVedtakFraTpsak.sak.saksnummer}"
            }
            return sakMedVedtakFraTpsak.toHentetSak()
        }

        val sakFraTpsak = sakRepo.hentForFnr(fnr)
        if (sakFraTpsak != null) {
            logger.debug { "Fant sak (uten vedtak) i TPSAK for fnr. sakId=${sakFraTpsak.id}, saksnummer=${sakFraTpsak.saksnummer}" }
            return sakFraTpsak.toHentetSak()
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
            ?.toHentetSak()
    }
}
