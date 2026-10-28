package no.nav.tiltakspenger.datadeling.vedtak.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet
import org.junit.jupiter.api.Test

class ValgtHjemmelHarIkkeRettighetDbJsonTest {

    @Test
    fun `leser lagrede hjemler`() {
        """["ALDER","RETT_TIL_DAGPENGER"]""".tilValgteHjemlerHarIkkeRettighet() shouldBe listOf(
            ValgtHjemmelHarIkkeRettighet.ALDER,
            ValgtHjemmelHarIkkeRettighet.RETT_TIL_DAGPENGER,
        )
    }

    @Test
    fun `leser gammelt navn på rett til dagpenger`() {
        """["FORLENGET_VENTETID"]""".tilValgteHjemlerHarIkkeRettighet() shouldBe listOf(
            ValgtHjemmelHarIkkeRettighet.RETT_TIL_DAGPENGER,
        )
    }
}
