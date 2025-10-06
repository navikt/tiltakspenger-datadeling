package no.nav.tiltakspenger.datadeling.testdata

import no.nav.tiltakspenger.datadeling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

object MeldeperiodeMother {
    fun meldeperiode(
        id: MeldeperiodeId = MeldeperiodeId.random(),
        periode: Periode = periode(),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        fnr: Fnr = Fnr.fromString("12345678901"),
        sakId: SakId = SakId.random(),
        saksnummer: String = "saksnummer",
        opprettet: LocalDateTime? = null,
        girRett: Map<LocalDate, Boolean> = periode.tilGirRett(),
        antallDagerForPeriode: Int = girRett.filter { it.value }.size,
    ): Meldeperiode {
        require(MeldeperiodeKjedeId.fraPeriode(periode) == kjedeId) {
            "KjedeId må være lik MeldeperiodeKjedeId.fraPeriode(periode)"
        }
        return Meldeperiode(
            id = id,
            kjedeId = kjedeId.verdi,
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
            opprettet = opprettet ?: LocalDateTime.now(),
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
            maksAntallDagerForPeriode = antallDagerForPeriode,
            girRett = girRett,
        )
    }

    fun periode(fraSisteMandagFor: LocalDate = LocalDate.now(), tilSisteSondagEtter: LocalDate? = null): Periode {
        if (tilSisteSondagEtter != null) {
            return tilSisteSondagEtter.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).let { sondag ->
                Periode(sondag.minusDays(13), sondag)
            }
        }

        return fraSisteMandagFor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { mandag ->
            Periode(mandag, mandag.plusDays(13))
        }
    }

    private fun Periode.tilGirRett(): Map<LocalDate, Boolean> = tilDager()
        .associateWith { value -> listOf(value.dayOfWeek).none { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } }
}
