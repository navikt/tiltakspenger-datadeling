package no.nav.tiltakspenger.datadeling.testutils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

/**
 * Verifiserer at kontraktsvalidatoren både godtar gyldige responser og faktisk feiler på avvik.
 * Det siste er poenget: en validator som aldri feiler gir falsk trygghet i route-testene.
 */
internal class VerifiserMotOpenApiKontraktTest {

    private fun verifiser(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        sti: String = "/vedtak/perioder",
        contentType: ContentType? = ContentType.Application.Json,
    ) = verifiserMotOpenApiKontrakt(
        metode = HttpMethod.Post,
        sti = sti,
        status = status,
        contentType = contentType,
        body = body,
    )

    // language=JSON
    private val gyldigVedtak = """
        {
          "vedtakId": "vedtak-1",
          "rettighet": "TILTAKSPENGER_OG_BARNETILLEGG",
          "periode": {"fraOgMed": "2024-01-01", "tilOgMed": "2024-03-01"},
          "kilde": "TPSAK",
          "barnetillegg": {"perioder": [{"antallBarn": 1, "periode": {"fraOgMed": "2024-01-01", "tilOgMed": "2024-03-01"}}]},
          "sats": 285,
          "satsBarnetillegg": 53,
          "vedtaksperiode": {"fraOgMed": "2024-01-01", "tilOgMed": "2024-03-01"},
          "innvilgelsesperioder": [{"fraOgMed": "2024-01-01", "tilOgMed": "2024-03-01"}],
          "omgjortAvRammevedtakId": null,
          "omgjorRammevedtakId": null,
          "vedtakstidspunkt": "2024-01-01T00:00:00+01:00"
        }
    """.trimIndent()

    @Test
    fun `gyldig vedtaksliste passerer`() {
        verifiser("[$gyldigVedtak]")
    }

    @Test
    fun `tom liste passerer`() {
        verifiser("[]")
    }

    @Test
    fun `null i anyOf-union passerer`() {
        verifiser("[${gyldigVedtak.replace("""{"perioder": [{"antallBarn": 1, "periode": {"fraOgMed": "2024-01-01", "tilOgMed": "2024-03-01"}}]}""", "null")}]")
    }

    @Test
    fun `ukjent enum-verdi feiler`() {
        shouldThrow<AssertionError> {
            verifiser("[${gyldigVedtak.replace("TILTAKSPENGER_OG_BARNETILLEGG", "UKJENT_RETTIGHET")}]")
        }.message shouldContain "enum"
    }

    @Test
    fun `manglende paakrevd felt feiler`() {
        shouldThrow<AssertionError> {
            verifiser("[${gyldigVedtak.replace("\"kilde\": \"TPSAK\",", "")}]")
        }.message shouldContain "mangler påkrevd felt 'kilde'"
    }

    @Test
    fun `udeklarert felt feiler`() {
        shouldThrow<AssertionError> {
            verifiser("[${gyldigVedtak.replace("\"sats\": 285,", "\"sats\": 285, \"smugla\": true,")}]")
        }.message shouldContain "'smugla' er ikke deklarert"
    }

    @Test
    fun `feil datatype feiler`() {
        shouldThrow<AssertionError> {
            verifiser("[${gyldigVedtak.replace("\"sats\": 285,", "\"sats\": \"to hundre\",")}]")
        }.message shouldContain "sats"
    }

    @Test
    fun `ugyldig datoformat feiler`() {
        shouldThrow<AssertionError> {
            verifiser("[${gyldigVedtak.replace("\"fraOgMed\": \"2024-01-01\"", "\"fraOgMed\": \"01.01.2024\"")}]")
        }.message shouldContain "gyldig date"
    }

    @Test
    fun `objekt i stedet for liste feiler`() {
        shouldThrow<AssertionError> {
            verifiser(gyldigVedtak)
        }.message shouldContain "forventet type [array]"
    }

    @Test
    fun `MappingError-respons paa 400 passerer`() {
        verifiser("""{"feilmelding": "noe er feil"}""", status = HttpStatusCode.BadRequest)
    }

    @Test
    fun `deklarert status uten innhold validerer ikke body`() {
        // 403 er deklarert uten content i kontrakten; dagens ErrorJson-body slipper derfor gjennom.
        verifiser("""{"melding": "hva som helst", "kode": "hva_som_helst"}""", status = HttpStatusCode.Forbidden)
    }

    @Test
    fun `udeklarert status feiler`() {
        shouldThrow<AssertionError> {
            verifiser("""{"melding": "x", "kode": "y"}""", status = HttpStatusCode.InternalServerError)
        }.message shouldContain "deklarerer ikke status 500"
    }

    @Test
    fun `udeklarert sti feiler`() {
        shouldThrow<AssertionError> {
            verifiser("[]", sti = "/finnes-ikke")
        }.message shouldContain "deklarerer ikke POST /finnes-ikke"
    }

    @Test
    fun `manglende content-type feiler naar innhold er deklarert`() {
        shouldThrow<AssertionError> {
            verifiser("[]", contentType = null)
        }.message shouldContain "Content-Type"
    }
}
