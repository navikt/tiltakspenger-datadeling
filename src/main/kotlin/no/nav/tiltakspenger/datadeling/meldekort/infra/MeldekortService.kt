package no.nav.tiltakspenger.datadeling.meldekort.infra

import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekort
import no.nav.tiltakspenger.datadeling.meldekort.Meldeperiode
import no.nav.tiltakspenger.datadeling.meldekort.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.meldekort.infra.routes.MeldekortResponse
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.satser.Satser

class MeldekortService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
) {
    fun hentMeldekort(fnr: Fnr, periode: Periode): MeldekortResponse {
        val meldeperioderOgGodkjenteMeldekort = meldeperiodeRepo.hentMeldeperioderOgGodkjenteMeldekort(fnr, periode)
        return MeldekortResponse(
            meldekortKlareTilUtfylling = meldeperioderOgGodkjenteMeldekort.filter { it.godkjentMeldekort == null && it.meldeperiode.erKlarTilUtfylling }
                .map { it.meldeperiode.toMeldekortKlartTilUtfyllingDTO() }
                .sortedByDescending { it.fraOgMed },
            godkjenteMeldekort = meldeperioderOgGodkjenteMeldekort.mapNotNull { it.godkjentMeldekort?.toGodkjentMeldekortDTO() }
                .sortedByDescending { it.fraOgMed },
        )
    }

    private fun Meldeperiode.toMeldekortKlartTilUtfyllingDTO() = MeldekortResponse.MeldekortKlartTilUtfyllingDTO(
        id = id.toString(),
        kjedeId = kjedeId,
        opprettet = opprettet,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        maksAntallDagerForPeriode = maksAntallDagerForPeriode,
        girRett = girRett,
        kanFyllesUtFraOgMed = kanFyllesUtFraOgMed,
    )

    private fun GodkjentMeldekort.toGodkjentMeldekortDTO(): MeldekortResponse.GodkjentMeldekortDTO {
        val satser = Satser.sats(tilOgMed)
        return MeldekortResponse.GodkjentMeldekortDTO(
            meldekortbehandlingId = meldekortbehandlingId.toString(),
            kjedeId = kjedeId,
            mottattTidspunkt = mottattTidspunkt,
            vedtattTidspunkt = vedtattTidspunkt,
            behandletAutomatisk = behandletAutomatisk,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            meldekortdager = meldekortdager.map { it.toMeldekortdagerDTO() },
            status = if (korrigert) {
                MeldekortResponse.GodkjentMeldekortDTO.GodkjentMeldekortStatus.KORRIGERING
            } else {
                MeldekortResponse.GodkjentMeldekortDTO.GodkjentMeldekortStatus.SENDT_TIL_UTBETALING
            },
            journalpostId = journalpostId,
            totaltBelop = totaltBelop,
            sats = satser.sats,
            satsBarnetillegg = if (barnetillegg) {
                satser.satsBarnetillegg
            } else {
                null
            },
            korrigering = if (korrigert) {
                MeldekortResponse.GodkjentMeldekortDTO.Korrigering(
                    totalDifferanse = totalDifferanse!!,
                    resultat = if (totalDifferanse < 0) {
                        MeldekortResponse.GodkjentMeldekortDTO.Korrigering.KorrigeringResultat.REDUKSJON
                    } else if (totalDifferanse > 0) {
                        MeldekortResponse.GodkjentMeldekortDTO.Korrigering.KorrigeringResultat.OKNING
                    } else {
                        MeldekortResponse.GodkjentMeldekortDTO.Korrigering.KorrigeringResultat.INGEN_ENDRING
                    },
                )
            } else {
                null
            },
            opprettet = opprettet,
            sistEndret = sistEndret,
        )
    }

    private fun GodkjentMeldekort.MeldekortDag.toMeldekortdagerDTO() =
        MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag(
            dato = dato,
            status = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag.MeldekortDagStatus.valueOf(status.name),
            reduksjon = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag.Reduksjon.valueOf(reduksjon.name),
        )
}
