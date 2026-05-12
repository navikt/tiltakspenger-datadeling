package no.nav.tiltakspenger.datadeling.vedtak.datadeling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaClient
import no.nav.tiltakspenger.datadeling.client.arena.domene.Rettighet
import no.nav.tiltakspenger.datadeling.sak.db.SakRepo
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.datadeling.sak.dto.toSakDTO
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.HentSakResponse
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakDTO
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakTidslinjeResponse
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.toHentSakResponse
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.toVedtakDTO
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.toVedtakResponse
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.AVSLAG
import no.nav.tiltakspenger.datadeling.vedtak.domene.tilInnvilgetRammevedtakstidslinje
import no.nav.tiltakspenger.datadeling.vedtak.domene.tilRammevedtakstidslinje
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import java.time.Clock
import java.time.LocalDate

class VedtakService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
    private val sakRepo: SakRepo,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    /**
     * Merk at denne er reservert Arena og de ønsker at vi kun sender perioder bruker har rett til tiltakspenger.
     * Hvis ingen gjeldende vedtaksperioder gir rett lenger, ønsker de en tom liste.
     * Ref: https://nav-it.slack.com/archives/CC9GYTA2C/p1734512113726549
     */
    fun hentTpVedtak(
        fnr: Fnr,
        periode: Periode,
    ): List<TiltakspengeVedtakMedSak> {
        val alleVedtakMedSak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
        val sak = alleVedtakMedSak.firstOrNull()?.sak
        val alleVedtak = alleVedtakMedSak.map { it.vedtak }
        return alleVedtak.tilInnvilgetRammevedtakstidslinje()
            .map { it.verdi.krympVirkningsperiode(it.periode) }
            .verdier
            .map {
                TiltakspengeVedtakMedSak(
                    sak = sak!!,
                    vedtak = it,
                )
            }
    }

    suspend fun hentTidslinjeOgAlleVedtak(
        fnr: Fnr,
        periode: Periode,
    ): VedtakTidslinjeResponse {
        val alleVedtakMedSak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
        val tpSak = alleVedtakMedSak.firstOrNull()?.sak
        val alleVedtak = alleVedtakMedSak.map { it.vedtak }
        val tidslinje = alleVedtak.tilRammevedtakstidslinje()
            // Vil kunne inneholde både innvilgelser (inkl. omgjøringer) og stans.
            .map { it.verdi.krympVirkningsperiode(it.periode) }
            .verdier

        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }

        val arenaSak = if (tpSak == null) {
            vedtakFraArena.sortedByDescending { it.beslutningsdato }.firstOrNull()?.sak
        } else {
            null
        }

        return VedtakTidslinjeResponse(
            tidslinje = tidslinje.toVedtakResponse(logger).sortedByDescending { it.vedtaksdato },
            alleVedtak = alleVedtak.toVedtakResponse(logger).sortedByDescending { it.vedtaksdato },
            vedtakFraArena = vedtakFraArena.map { it.toVedtakDTO() }.sortedByDescending { it.periode.tilOgMed },
            sak = tpSak?.toSakDTO() ?: arenaSak?.toSakDTO(),
        )
    }

    /**
     * Henter alle vedtak som påvirker rett til tiltakspenger i perioden fra både TPSAK og Arena.
     * Avslag fra tp-sak ekskluderes.
     */
    suspend fun hentVedtaksperioder(
        fnr: Fnr,
        periode: Periode,
    ): List<VedtakDTO> {
        val vedtakFraTpsak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
            .filter { it.vedtak.rettighet != AVSLAG }
            .map { it.vedtak.toVedtakDTO(logger) }
        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }
            .map { it.toVedtakDTO() }

        return (vedtakFraArena + vedtakFraTpsak)
    }

    /**
     * Henter sak for en bruker basert på fnr.
     * Søker først i TPSAK, og hvis ikke funnet, søker i Arena.
     */
    suspend fun hentSak(fnr: Fnr): HentSakResponse? {
        val sakMedVedtakFraTpsak = vedtakRepo.hentSakMedVedtakForFnr(fnr)
        if (sakMedVedtakFraTpsak != null) {
            logger.debug {
                "Fant sak med vedtak i TPSAK for fnr. sakId=${sakMedVedtakFraTpsak.sak.id}, saksnummer=${sakMedVedtakFraTpsak.sak.saksnummer}"
            }
            return sakMedVedtakFraTpsak.toHentSakResponse(clock)
        }

        val sakFraTpsak: Sak? = sakRepo.hentForFnr(fnr)
        if (sakFraTpsak != null && (sakFraTpsak.rammevedtak.isNotEmpty() || sakFraTpsak.behandlinger.isNotEmpty())) {
            logger.debug { "Fant sak (uten vedtak via vedtakRepo, men med behandlinger/vedtak via sakRepo) i TPSAK for fnr. sakId=${sakFraTpsak.id}, saksnummer=${sakFraTpsak.saksnummer}" }
            return sakFraTpsak.toHentSakResponse(clock)
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
            ?.toHentSakResponse()
    }
}
