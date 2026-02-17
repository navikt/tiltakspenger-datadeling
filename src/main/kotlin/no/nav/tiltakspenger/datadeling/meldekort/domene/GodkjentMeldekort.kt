package no.nav.tiltakspenger.datadeling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import java.time.LocalDate
import java.time.LocalDateTime

data class GodkjentMeldekort(
    val meldekortbehandlingId: MeldekortId,
    val kjedeId: String,
    val sakId: SakId,
    val meldeperiodeId: MeldeperiodeId,
    val mottattTidspunkt: LocalDateTime?,
    val vedtattTidspunkt: LocalDateTime,
    val behandletAutomatisk: Boolean,
    val korrigert: Boolean,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortdager: List<MeldekortDag>,
    val journalpostId: String,
    val totaltBelop: Int,
    val totalDifferanse: Int?,
    val barnetillegg: Boolean,
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
            FRAVAER_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
            FRAVAER_ANNET,
            IKKE_BESVART,
            IKKE_TILTAKSDAG,
            IKKE_RETT_TIL_TILTAKSPENGER,
        }
    }
}
