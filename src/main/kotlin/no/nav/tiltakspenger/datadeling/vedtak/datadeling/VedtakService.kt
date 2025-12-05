package no.nav.tiltakspenger.datadeling.vedtak.datadeling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.arena.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.dto.Sak
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakDTO
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakTidslinjeResponse
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.toVedtakDTO
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.toVedtakResponse
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.AVSLAG
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.STANS
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.TILTAKSPENGER
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import kotlin.collections.mapNotNull

class VedtakService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
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
    ): List<TiltakspengerVedtak> {
        val alleVedtak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
        return hentInnvilgetTidslinje(alleVedtak)
            .map { it.verdi.krympVirkningsperiode(it.periode) }
            .verdier
    }

    suspend fun hentTidslinjeOgAlleVedtak(
        fnr: Fnr,
        periode: Periode,
    ): VedtakTidslinjeResponse {
        val alleVedtak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
        val tidslinje = hentTidslinje(alleVedtak)
            // Vil kunne inneholde både innvilgelser (inkl. omgjøringer) og stans.
            .map { it.verdi.krympVirkningsperiode(it.periode) }
            .verdier

        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }
            .map { it.toVedtakDTO() }

        return VedtakTidslinjeResponse(
            tidslinje = tidslinje.toVedtakResponse(logger).sortedByDescending { it.vedtaksdato },
            alleVedtak = alleVedtak.toVedtakResponse(logger).sortedByDescending { it.vedtaksdato },
            vedtakFraArena = vedtakFraArena.sortedByDescending { it.periode.tilOgMed },
            sak = if (alleVedtak.isEmpty()) {
                null
            } else {
                Sak(
                    sakId = alleVedtak.first().sakId,
                    saksnummer = alleVedtak.first().saksnummer,
                )
            },
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
        val vedtakFraTpsak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
            .filter { it.rettighet != AVSLAG }
            .map { it.toVedtakDTO(logger) }
        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }
            .map { it.toVedtakDTO() }

        return (vedtakFraArena + vedtakFraTpsak)
    }

    /**
     * En periodisert liste over de gjeldende innvilgede vedtak i tp-sak.
     * Avslag er ekskludert fra tidslinjen. Og Stans/Opphør ekskluderes etter vi lager tidslinjen.
     * Vi fjerner også den delen av omgjøringsvedtak som ikke gir rett til tiltakspenger.
     */
    fun hentInnvilgetTidslinje(
        alleVedtak: List<TiltakspengerVedtak>,
    ): Periodisering<TiltakspengerVedtak> {
        return hentTidslinje(alleVedtak)
            .mapNotNull { (vedtak, gjeldendePeriode) ->
                when (vedtak.rettighet) {
                    AVSLAG -> throw IllegalStateException("Avslag skal være filtrert vekk før innvilget tidslinje lages.")
                    STANS -> null
                    TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG -> {
                        // Omgjøringsvedtak kan ha en innvilgelsesperiode som er mindre enn virkningsperioden (implisitt ikke lenger rett).
                        gjeldendePeriode.overlappendePeriode(vedtak.innvilgelsesperiode!!)?.let { overlappendePeriode ->
                            PeriodeMedVerdi(vedtak, overlappendePeriode)
                        }
                    }
                }
            }.tilPeriodisering()
    }

    /**
     * En periodisert liste over gjeldende vedtak i tp-sak.
     * Avslag skal være ekskludert.
     * Fjerner vedtak som er omgjort i sin helhet.
     */
    fun hentTidslinje(
        alleVedtak: List<TiltakspengerVedtak>,
    ): Periodisering<TiltakspengerVedtak> {
        return alleVedtak.filter {
            when (it.rettighet) {
                TILTAKSPENGER, TILTAKSPENGER_OG_BARNETILLEGG, STANS -> true
                // Rene søknadsbehandlingsavslag påvirker ikke retten din til tiltakspenger.
                AVSLAG -> false
            }
        }
            // Fjerner alle vedtak som er omgjort i sin helhet av et annet vedtak.
            .filter { it.omgjortAvRammevedtakId == null }
            .toTidslinje()
    }
}
