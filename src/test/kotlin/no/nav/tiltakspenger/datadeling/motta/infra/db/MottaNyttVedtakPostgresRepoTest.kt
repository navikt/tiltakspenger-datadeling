package no.nav.tiltakspenger.datadeling.motta.infra.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.felles.VedtakMother
import no.nav.tiltakspenger.datadeling.felles.withMigratedDb
import org.junit.jupiter.api.Test

class MottaNyttVedtakPostgresRepoTest {

    @Test
    fun `kan lagre og hente vedtak`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.mottaNyttVedtakPostgresRepo

            val vedtak = VedtakMother.tiltakspengerVedtak()
            repo.lagre(vedtak)
            testDataHelper.sessionFactory.withSession { session ->
                repo.hentForVedtakIdOgKilde(vedtak.vedtakId, vedtak.kilde, session) shouldBe vedtak
            }
        }
    }
}
