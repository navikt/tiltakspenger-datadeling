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
        val sakId: String,
        val saksnummer: String,
        val opprettet: LocalDateTime,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val maksAntallDagerForPeriode: Int,
        val girRett: Map<LocalDate, Boolean>,
        val kanFyllesUtFraOgMed: LocalDate,
    )

    data class GodkjentMeldekortDTO(
        val kjedeId: String,
        val sakId: String,
        val meldeperiodeId: String,
        val saksnummer: String,
        val mottattTidspunkt: LocalDateTime?,
        val vedtattTidspunkt: LocalDateTime,
        val behandletAutomatisk: Boolean,
        val korrigert: Boolean,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val meldekortdager: List<MeldekortDag>,
        val opprettet: LocalDateTime,
        val sistEndret: LocalDateTime,
    ) {
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
                FRAVAER_ANNET,
                IKKE_BESVART,
                IKKE_TILTAKSDAG,
                IKKE_RETT_TIL_TILTAKSPENGER,
            }
        }
    }
}
