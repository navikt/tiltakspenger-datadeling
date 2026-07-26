package no.nav.tiltakspenger.datadeling.meldekort.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortIKjede
import no.nav.tiltakspenger.datadeling.meldekort.Meldekortoversikt
import no.nav.tiltakspenger.datadeling.testdata.MeldekortMother
import no.nav.tiltakspenger.datadeling.testdata.MeldeperiodeMother
import no.nav.tiltakspenger.libs.satser.Satser
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Mappingen bak `POST /meldekort/detaljer`.
 * Korrigeringsresultatet utledes av fortegnet på differansen, og barnetilleggssatsen tas kun med når vedtaket har barnetillegg.
 */
class MeldekortResponseTest {

    private val meldeperiode = MeldeperiodeMother.meldeperiode()

    private fun oversikt(
        korrigert: Boolean = false,
        totalDifferanse: Int? = null,
        barnetillegg: Boolean = true,
    ): Meldekortoversikt {
        val behandling = MeldekortMother.godkjentMeldekort(
            meldeperiode = meldeperiode,
            korrigert = korrigert,
            totalDifferanse = totalDifferanse,
            barnetillegg = barnetillegg,
        )
        return Meldekortoversikt(
            meldeperioderKlareTilUtfylling = emptyList(),
            godkjenteMeldekort = listOf(
                GodkjentMeldekortIKjede(behandling = behandling, meldeperiode = behandling.meldeperioder.single()),
            ),
        )
    }

    @Test
    fun `uten korrigering er statusen SENDT_TIL_UTBETALING og korrigering er null`() {
        val respons = oversikt().toMeldekortResponse().godkjenteMeldekort.single()

        respons.status shouldBe MeldekortResponse.GodkjentMeldekortDTO.GodkjentMeldekortStatus.SENDT_TIL_UTBETALING
        respons.korrigering shouldBe null
    }

    @ParameterizedTest(name = "differanse {0} gir resultat {1}")
    @CsvSource(
        "-500, REDUKSJON",
        "500, OKNING",
        "0, INGEN_ENDRING",
    )
    fun `korrigering utleder resultatet av fortegnet pa differansen`(differanse: Int, forventet: String) {
        val respons = oversikt(korrigert = true, totalDifferanse = differanse)
            .toMeldekortResponse().godkjenteMeldekort.single()

        respons.status shouldBe MeldekortResponse.GodkjentMeldekortDTO.GodkjentMeldekortStatus.KORRIGERING
        respons.korrigering shouldBe MeldekortResponse.GodkjentMeldekortDTO.Korrigering(
            totalDifferanse = differanse,
            resultat = MeldekortResponse.GodkjentMeldekortDTO.Korrigering.KorrigeringResultat.valueOf(forventet),
        )
    }

    @Test
    fun `satsBarnetillegg tas med nar vedtaket har barnetillegg, ellers er den null`() {
        val medBarnetillegg = oversikt(barnetillegg = true).toMeldekortResponse().godkjenteMeldekort.single()
        val utenBarnetillegg = oversikt(barnetillegg = false).toMeldekortResponse().godkjenteMeldekort.single()

        medBarnetillegg.satsBarnetillegg shouldBe Satser.sats(meldeperiode.tilOgMed).satsBarnetillegg
        utenBarnetillegg.satsBarnetillegg shouldBe null
    }

    @Test
    fun `meldeperioder klare til utfylling mappes med kanFyllesUtFraOgMed`() {
        val respons = Meldekortoversikt(
            meldeperioderKlareTilUtfylling = listOf(meldeperiode),
            godkjenteMeldekort = emptyList(),
        ).toMeldekortResponse()

        val klar = respons.meldekortKlareTilUtfylling.single()
        klar.id shouldBe meldeperiode.id.toString()
        klar.kjedeId shouldBe meldeperiode.kjedeId
        klar.kanFyllesUtFraOgMed shouldBe meldeperiode.kanFyllesUtFraOgMed
        klar.maksAntallDagerForPeriode shouldBe meldeperiode.maksAntallDagerForPeriode
    }
}
