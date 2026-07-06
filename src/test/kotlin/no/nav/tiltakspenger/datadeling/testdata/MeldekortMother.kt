package no.nav.tiltakspenger.datadeling.testdata

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandling
import no.nav.tiltakspenger.datadeling.meldekort.Meldeperiode
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
    ): GodkjentMeldekortbehandling {
        return GodkjentMeldekortbehandling(
            meldekortbehandlingId = meldekortbehandlingId,
            sakId = meldeperiode.sakId,
            meldeperioder = nonEmptyListOf(
                GodkjentMeldekortbehandling.Meldeperiode(
                    kjedeId = meldeperiode.kjedeId,
                    meldeperiodeId = meldeperiode.id.toString(),
                    korrigert = korrigert,
                    meldekortdager = meldeperiode.toMeldekortDager(),
                    totaltBelop = totaltBelop,
                    totalDifferanse = totalDifferanse,
                    fraOgMed = meldeperiode.fraOgMed,
                    tilOgMed = meldeperiode.tilOgMed,
                    mottattTidspunkt = mottattTidspunkt,
                ),
            ),
            vedtattTidspunkt = vedtattTidspunkt,
            behandletAutomatisk = behandletAutomatisk,
            fraOgMed = meldeperiode.fraOgMed,
            tilOgMed = meldeperiode.tilOgMed,
            journalpostId = journalpostId,
            totaltBelop = totaltBelop,
            totalDifferanse = totalDifferanse,
            barnetillegg = barnetillegg,
            opprettet = opprettet,
            sistEndret = sistEndret,
        )
    }

    fun Meldeperiode.toMeldekortDager(): NonEmptyList<GodkjentMeldekortbehandling.MeldekortDag> {
        return this.girRett.map {
            if (it.value) {
                GodkjentMeldekortbehandling.MeldekortDag(
                    dato = it.key,
                    status = GodkjentMeldekortbehandling.MeldekortDag.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET,
                    reduksjon = GodkjentMeldekortbehandling.MeldekortDag.Reduksjon.INGEN_REDUKSJON,
                )
            } else {
                GodkjentMeldekortbehandling.MeldekortDag(
                    dato = it.key,
                    status = GodkjentMeldekortbehandling.MeldekortDag.MeldekortDagStatus.IKKE_TILTAKSDAG,
                    reduksjon = GodkjentMeldekortbehandling.MeldekortDag.Reduksjon.YTELSEN_FALLER_BORT,
                )
            }
        }.toNonEmptyListOrThrow()
    }
}
