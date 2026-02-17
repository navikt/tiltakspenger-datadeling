package no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes

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
            val resultat: String,
        )

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
