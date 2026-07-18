package no.nav.tiltakspenger.datadeling.meldekort.infra

import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandling
import no.nav.tiltakspenger.datadeling.meldekort.Meldeperiode
import no.nav.tiltakspenger.datadeling.meldekort.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.meldekort.infra.routes.MeldekortResponse
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.satser.Satser
import java.time.Clock

class MeldekortService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val clock: Clock,
) {
    fun hentMeldekort(fnr: Fnr, periode: Periode): MeldekortResponse {
        val meldeperioderOgGodkjenteMeldekort = meldeperiodeRepo.hentMeldeperioderOgGodkjenteMeldekort(fnr, periode)

        return MeldekortResponse(
            meldekortKlareTilUtfylling = meldeperioderOgGodkjenteMeldekort.filter { it.godkjentMeldekortbehandling == null && it.meldeperiode.erKlarTilUtfylling(clock) }
                .map { it.meldeperiode.toMeldekortKlartTilUtfyllingDTO() }
                .sortedByDescending { it.fraOgMed },
            godkjenteMeldekort = meldeperioderOgGodkjenteMeldekort
                .mapNotNull { rad ->
                    rad.godkjentMeldekortbehandling?.toGodkjentMeldekortDTO(rad.meldeperiode.kjedeId)
                }
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

    private fun GodkjentMeldekortbehandling.toGodkjentMeldekortDTO(kjedeId: String): MeldekortResponse.GodkjentMeldekortDTO {
        val meldeperiode = meldeperioder.single { it.kjedeId == kjedeId }
        val satser = Satser.sats(meldeperiode.tilOgMed)
        return MeldekortResponse.GodkjentMeldekortDTO(
            meldekortbehandlingId = meldekortbehandlingId.toString(),
            kjedeId = meldeperiode.kjedeId,
            mottattTidspunkt = meldeperiode.mottattTidspunkt,
            vedtattTidspunkt = vedtattTidspunkt,
            behandletAutomatisk = behandletAutomatisk,
            fraOgMed = meldeperiode.fraOgMed,
            tilOgMed = meldeperiode.tilOgMed,
            meldekortdager = meldeperiode.meldekortdager.map { it.toMeldekortdagerDTO() },
            status = if (meldeperiode.korrigert) {
                MeldekortResponse.GodkjentMeldekortDTO.GodkjentMeldekortStatus.KORRIGERING
            } else {
                MeldekortResponse.GodkjentMeldekortDTO.GodkjentMeldekortStatus.SENDT_TIL_UTBETALING
            },
            journalpostId = journalpostId,
            totaltBelop = meldeperiode.totaltBelop,
            sats = satser.sats,
            satsBarnetillegg = if (barnetillegg) {
                satser.satsBarnetillegg
            } else {
                null
            },
            korrigering = if (meldeperiode.korrigert) {
                MeldekortResponse.GodkjentMeldekortDTO.Korrigering(
                    totalDifferanse = meldeperiode.totalDifferanse!!,
                    resultat = if (meldeperiode.totalDifferanse < 0) {
                        MeldekortResponse.GodkjentMeldekortDTO.Korrigering.KorrigeringResultat.REDUKSJON
                    } else if (meldeperiode.totalDifferanse > 0) {
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

    private fun GodkjentMeldekortbehandling.MeldekortDag.toMeldekortdagerDTO() =
        MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag(
            dato = dato,
            status = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag.MeldekortDagStatus.valueOf(status.name),
            reduksjon = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag.Reduksjon.valueOf(reduksjon.name),
        )
}
