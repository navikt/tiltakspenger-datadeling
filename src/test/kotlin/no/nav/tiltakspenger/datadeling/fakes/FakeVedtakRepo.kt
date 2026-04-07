@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.datadeling.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDateTime

class FakeVedtakRepo : VedtakRepo {
    private val vedtak = Atomic(mutableMapOf<String, TiltakspengerVedtak>())

    override fun lagre(vedtak: TiltakspengerVedtak) {
        this.vedtak.get()[vedtak.vedtakId] = vedtak
    }

    override fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<TiltakspengeVedtakMedSak> = emptyList()

    override fun hentForFnr(fnr: Fnr): List<TiltakspengeVedtakMedSak> = emptyList()

    override fun hentRammevedtakSomSkalDelesMedObo(limit: Int): List<TiltakspengeVedtakMedSak> = emptyList()

    override fun markerSendtTilObo(vedtakId: String, tidspunkt: LocalDateTime) {}

    fun alle(): List<TiltakspengerVedtak> = vedtak.get().values.toList()
}
