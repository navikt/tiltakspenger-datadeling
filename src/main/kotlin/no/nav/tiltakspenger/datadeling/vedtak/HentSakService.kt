package no.nav.tiltakspenger.datadeling.vedtak

import arrow.core.Either
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.httpklient.loggSuksess
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.Clock
import java.time.LocalDate

class HentSakService(
    private val hentSakRepo: HentSakRepo,
    private val arenaClient: ArenaClient,
    private val clock: Clock,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Henter sak for en bruker basert på fnr.
     * Søker først i TPSAK, og hvis ikke funnet, søker i Arena.
     */
    suspend fun hentSak(fnr: Fnr): Either<HttpKlientError, HentetSak?> {
        val sakFraTpsak = hentSakRepo.hentSakForVedtakSak(fnr)
        if (sakFraTpsak != null && sakFraTpsak.harInnhold) {
            logger.debug { "Fant sak i TPSAK for fnr. sakId=${sakFraTpsak.id}, saksnummer=${sakFraTpsak.saksnummer}" }
            return sakFraTpsak.toHentetSak(clock).right()
        }
        if (sakFraTpsak != null) {
            logger.debug { "Fant tom sak i TPSAK for fnr. Behandler som ikke funnet. sakId=${sakFraTpsak.id}, saksnummer=${sakFraTpsak.saksnummer}" }
        }

        logger.debug { "Fant ingen sak med behandling/vedtak i TPSAK, søker i Arena" }
        return arenaClient.hentVedtak(
            fnr,
            Periode(LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31)),
        )
            .onLeft { it.loggFeil(logger, "henting av vedtak fra Arena", "Sak-oppslag, åpen periode", sikkerlogg) }
            .onRight { it.loggSuksess(logger, "Hentet vedtak fra Arena.", sikkerlogg) }
            .map { respons ->
                respons.body
                    .sortedByDescending { it.beslutningsdato }
                    .firstOrNull()
                    ?.sak
                    ?.toHentetSak()
            }
    }
}
