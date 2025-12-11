package no.nav.tiltakspenger.datadeling.testdata

import no.nav.tiltakspenger.datadeling.meldekort.domene.GodkjentMeldekort
import no.nav.tiltakspenger.datadeling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.libs.common.MeldekortId
import java.time.LocalDateTime

object MeldekortMother {
    fun godkjentMeldekort(
        meldeperiode: Meldeperiode,
        meldekortbehandlingId: MeldekortId = MeldekortId.random(),
        mottattTidspunkt: LocalDateTime? = LocalDateTime.now().minusHours(1),
        vedtattTidspunkt: LocalDateTime = LocalDateTime.now(),
        behandletAutomatisk: Boolean = true,
        korrigert: Boolean = false,
        journalpostId: String = "jpid",
        totaltBelop: Int = 4560,
        totalDifferanse: Int? = 98,
        barnetillegg: Boolean = true,
        opprettet: LocalDateTime = LocalDateTime.now(),
        sistEndret: LocalDateTime = LocalDateTime.now(),
    ): GodkjentMeldekort {
        return GodkjentMeldekort(
            meldekortbehandlingId = meldekortbehandlingId,
            kjedeId = meldeperiode.kjedeId,
            sakId = meldeperiode.sakId,
            meldeperiodeId = meldeperiode.id,
            mottattTidspunkt = mottattTidspunkt,
            vedtattTidspunkt = vedtattTidspunkt,
            behandletAutomatisk = behandletAutomatisk,
            korrigert = korrigert,
            fraOgMed = meldeperiode.fraOgMed,
            tilOgMed = meldeperiode.tilOgMed,
            meldekortdager = meldeperiode.toMeldekortDager(),
            journalpostId = journalpostId,
            totaltBelop = totaltBelop,
            totalDifferanse = totalDifferanse,
            barnetillegg = barnetillegg,
            opprettet = opprettet,
            sistEndret = sistEndret,
        )
    }

    fun Meldeperiode.toMeldekortDager(): List<GodkjentMeldekort.MeldekortDag> {
        return this.girRett.map {
            if (it.value) {
                GodkjentMeldekort.MeldekortDag(
                    dato = it.key,
                    status = GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET,
                    reduksjon = GodkjentMeldekort.MeldekortDag.Reduksjon.INGEN_REDUKSJON,
                )
            } else {
                GodkjentMeldekort.MeldekortDag(
                    dato = it.key,
                    status = GodkjentMeldekort.MeldekortDag.MeldekortDagStatus.IKKE_TILTAKSDAG,
                    reduksjon = GodkjentMeldekort.MeldekortDag.Reduksjon.YTELSEN_FALLER_BORT,
                )
            }
        }
    }
}
