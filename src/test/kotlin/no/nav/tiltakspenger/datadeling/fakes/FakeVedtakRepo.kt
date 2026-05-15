@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.datadeling.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.datadeling.sak.Sak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengeSakMedVedtak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.vedtak.VedtakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDateTime

class FakeVedtakRepo : VedtakRepo {
    private val vedtak = Atomic(mutableMapOf<String, TiltakspengerVedtak>())

    override fun lagre(vedtak: TiltakspengerVedtak) {
        this.vedtak.get()[vedtak.vedtakId] = vedtak
    }

    override fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<TiltakspengeVedtakMedSak> = emptyList()

    override fun hentForFnr(fnr: Fnr): List<TiltakspengeVedtakMedSak> = emptyList()
    override fun hentSakMedVedtakForFnr(fnr: Fnr): TiltakspengeSakMedVedtak? {
        val alleVedtak = vedtak.get().values.toList()
        if (alleVedtak.isEmpty()) return null
        val førsteVedtak = alleVedtak.minBy { it.opprettet }
        return TiltakspengeSakMedVedtak(
            sak = Sak(
                id = førsteVedtak.sakId,
                fnr = fnr,
                saksnummer = førsteVedtak.saksnummer,
                opprettet = førsteVedtak.opprettet,
            ),
            vedtak = alleVedtak,
        )
    }

    override fun hentRammevedtakSomSkalDelesMedObo(limit: Int): List<TiltakspengeVedtakMedSak> = emptyList()

    override fun markerSendtTilObo(vedtakId: String, tidspunkt: LocalDateTime) {}

    fun alle(): List<TiltakspengerVedtak> = vedtak.get().values.toList()
}
