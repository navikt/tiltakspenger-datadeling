package no.nav.tiltakspenger.datadeling.meldekort.datadeling

import no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes.MeldekortResponse
import no.nav.tiltakspenger.datadeling.meldekort.db.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.meldekort.domene.GodkjentMeldekort
import no.nav.tiltakspenger.datadeling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

class MeldekortService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
) {
    fun hentMeldekort(fnr: Fnr, periode: Periode): MeldekortResponse {
        val meldeperioderOgGodkjenteMeldekort = meldeperiodeRepo.hentMeldeperioderOgGodkjenteMeldekort(fnr, periode)
        return MeldekortResponse(
            meldekortKlareTilUtfylling = meldeperioderOgGodkjenteMeldekort.filter { it.godkjentMeldekort == null && it.meldeperiode.erKlarTilUtfylling }
                .map { it.meldeperiode.toMeldekortKlartTilUtfyllingDTO() },
            godkjenteMeldekort = meldeperioderOgGodkjenteMeldekort.mapNotNull { it.godkjentMeldekort?.toGodkjentMeldekortDTO() },
        )
    }

    private fun Meldeperiode.toMeldekortKlartTilUtfyllingDTO() = MeldekortResponse.MeldekortKlartTilUtfyllingDTO(
        id = id.toString(),
        kjedeId = kjedeId,
        sakId = sakId.toString(),
        saksnummer = saksnummer,
        opprettet = opprettet,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        maksAntallDagerForPeriode = maksAntallDagerForPeriode,
        girRett = girRett,
        kanFyllesUtFraOgMed = kanFyllesUtFraOgMed,
    )

    private fun GodkjentMeldekort.toGodkjentMeldekortDTO() = MeldekortResponse.GodkjentMeldekortDTO(
        kjedeId = kjedeId,
        sakId = sakId.toString(),
        meldeperiodeId = meldeperiodeId.toString(),
        saksnummer = saksnummer,
        mottattTidspunkt = mottattTidspunkt,
        vedtattTidspunkt = vedtattTidspunkt,
        behandletAutomatisk = behandletAutomatisk,
        korrigert = korrigert,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        meldekortdager = meldekortdager.map { it.toMeldekortdagerDTO() },
        opprettet = opprettet,
        sistEndret = sistEndret,
    )

    private fun GodkjentMeldekort.MeldekortDag.toMeldekortdagerDTO() = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag(
        dato = dato,
        status = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag.MeldekortDagStatus.valueOf(status.name),
        reduksjon = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag.Reduksjon.valueOf(reduksjon.name),
    )
}
