package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

data class ArenaVedtak(
    override val periode: Periode,
    override val rettighet: Vedtak.Rettighet,
    override val vedtakId: String,
    override val sakId: String,
    override val saksnummer: String?,
    override val fnr: Fnr,
) : Vedtak {
    override val kilde: String = "arena"
}
