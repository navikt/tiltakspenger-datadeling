package no.nav.tiltakspenger.datadeling.testdata

import no.nav.tiltakspenger.datadeling.meldekort.domene.GodkjentMeldekort
import no.nav.tiltakspenger.datadeling.meldekort.domene.Meldeperiode
import java.time.LocalDateTime

object MeldekortMother {
    fun godkjentMeldekort(
        meldeperiode: Meldeperiode,
        mottattTidspunkt: LocalDateTime? = LocalDateTime.now().minusHours(1),
        vedtattTidspunkt: LocalDateTime = LocalDateTime.now(),
        behandletAutomatisk: Boolean = true,
        korrigert: Boolean = false,
        opprettet: LocalDateTime = LocalDateTime.now(),
        sistEndret: LocalDateTime = LocalDateTime.now(),
    ): GodkjentMeldekort {
        return GodkjentMeldekort(
            kjedeId = meldeperiode.kjedeId,
            sakId = meldeperiode.sakId,
            meldeperiodeId = meldeperiode.id,
            fnr = meldeperiode.fnr,
            saksnummer = meldeperiode.saksnummer,
            mottattTidspunkt = mottattTidspunkt,
            vedtattTidspunkt = vedtattTidspunkt,
            behandletAutomatisk = behandletAutomatisk,
            korrigert = korrigert,
            fraOgMed = meldeperiode.fraOgMed,
            tilOgMed = meldeperiode.tilOgMed,
            meldekortdager = meldeperiode.toMeldekortDager(),
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
