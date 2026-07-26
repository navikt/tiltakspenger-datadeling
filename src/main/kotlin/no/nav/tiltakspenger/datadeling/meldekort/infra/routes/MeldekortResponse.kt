package no.nav.tiltakspenger.datadeling.meldekort.infra.routes

import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortIKjede
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandling
import no.nav.tiltakspenger.datadeling.meldekort.Meldekortoversikt
import no.nav.tiltakspenger.datadeling.meldekort.Meldeperiode
import no.nav.tiltakspenger.libs.satser.Satser
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortResponse(
    val meldekortKlareTilUtfylling: List<MeldekortKlartTilUtfyllingDTO>,
    val godkjenteMeldekort: List<GodkjentMeldekortDTO>,
) {
    data class MeldekortKlartTilUtfyllingDTO(
        val id: String,
        val kjedeId: String,
        val opprettet: LocalDateTime,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val maksAntallDagerForPeriode: Int,
        val girRett: Map<LocalDate, Boolean>,
        val kanFyllesUtFraOgMed: LocalDate,
    )

    data class GodkjentMeldekortDTO(
        val meldekortbehandlingId: String,
        val kjedeId: String,
        val mottattTidspunkt: LocalDateTime?,
        val vedtattTidspunkt: LocalDateTime,
        val behandletAutomatisk: Boolean,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val meldekortdager: List<MeldekortDag>,
        val status: GodkjentMeldekortStatus,
        val journalpostId: String,
        val totaltBelop: Int,
        val sats: Int,
        val satsBarnetillegg: Int?,
        val korrigering: Korrigering?,
        val opprettet: LocalDateTime,
        val sistEndret: LocalDateTime,
    ) {
        enum class GodkjentMeldekortStatus {
            SENDT_TIL_UTBETALING,
            KORRIGERING,
        }

        data class Korrigering(
            val totalDifferanse: Int,
            val resultat: KorrigeringResultat,
        ) {
            enum class KorrigeringResultat {
                REDUKSJON,
                OKNING,
                INGEN_ENDRING,
            }
        }

        data class MeldekortDag(
            val dato: LocalDate,
            val status: MeldekortDagStatus,
            val reduksjon: Reduksjon,
        ) {
            enum class Reduksjon {
                INGEN_REDUKSJON,
                UKJENT,
                YTELSEN_FALLER_BORT,
            }
            enum class MeldekortDagStatus {
                DELTATT_UTEN_LONN_I_TILTAKET,
                DELTATT_MED_LONN_I_TILTAKET,
                FRAVAER_SYK,
                FRAVAER_SYKT_BARN,
                FRAVAER_GODKJENT_AV_NAV,
                FRAVAER_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
                FRAVAER_ANNET,
                IKKE_BESVART,
                IKKE_TILTAKSDAG,
                IKKE_RETT_TIL_TILTAKSPENGER,
            }
        }
    }
}

internal fun Meldekortoversikt.toMeldekortResponse() = MeldekortResponse(
    meldekortKlareTilUtfylling = meldeperioderKlareTilUtfylling.map { it.toMeldekortKlartTilUtfyllingDTO() },
    godkjenteMeldekort = godkjenteMeldekort.map { it.toGodkjentMeldekortDTO() },
)

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

private fun GodkjentMeldekortIKjede.toGodkjentMeldekortDTO(): MeldekortResponse.GodkjentMeldekortDTO {
    val satser = Satser.sats(meldeperiode.tilOgMed)
    return MeldekortResponse.GodkjentMeldekortDTO(
        meldekortbehandlingId = behandling.meldekortbehandlingId.toString(),
        kjedeId = meldeperiode.kjedeId,
        mottattTidspunkt = meldeperiode.mottattTidspunkt,
        vedtattTidspunkt = behandling.vedtattTidspunkt,
        behandletAutomatisk = behandling.behandletAutomatisk,
        fraOgMed = meldeperiode.fraOgMed,
        tilOgMed = meldeperiode.tilOgMed,
        meldekortdager = meldeperiode.meldekortdager.map { it.toMeldekortdagerDTO() },
        status = if (meldeperiode.korrigert) {
            MeldekortResponse.GodkjentMeldekortDTO.GodkjentMeldekortStatus.KORRIGERING
        } else {
            MeldekortResponse.GodkjentMeldekortDTO.GodkjentMeldekortStatus.SENDT_TIL_UTBETALING
        },
        journalpostId = behandling.journalpostId,
        totaltBelop = meldeperiode.totaltBelop,
        sats = satser.sats,
        satsBarnetillegg = if (behandling.barnetillegg) {
            satser.satsBarnetillegg
        } else {
            null
        },
        korrigering = if (meldeperiode.korrigert) {
            meldeperiode.toKorrigeringDTO()
        } else {
            null
        },
        opprettet = behandling.opprettet,
        sistEndret = behandling.sistEndret,
    )
}

private fun GodkjentMeldekortbehandling.Meldeperiode.toKorrigeringDTO(): MeldekortResponse.GodkjentMeldekortDTO.Korrigering {
    val totalDifferanse = this.totalDifferanse!!
    return MeldekortResponse.GodkjentMeldekortDTO.Korrigering(
        totalDifferanse = totalDifferanse,
        resultat = if (totalDifferanse < 0) {
            MeldekortResponse.GodkjentMeldekortDTO.Korrigering.KorrigeringResultat.REDUKSJON
        } else if (totalDifferanse > 0) {
            MeldekortResponse.GodkjentMeldekortDTO.Korrigering.KorrigeringResultat.OKNING
        } else {
            MeldekortResponse.GodkjentMeldekortDTO.Korrigering.KorrigeringResultat.INGEN_ENDRING
        },
    )
}

private fun GodkjentMeldekortbehandling.MeldekortDag.toMeldekortdagerDTO() =
    MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag(
        dato = dato,
        status = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag.MeldekortDagStatus.valueOf(status.name),
        reduksjon = MeldekortResponse.GodkjentMeldekortDTO.MeldekortDag.Reduksjon.valueOf(reduksjon.name),
    )
