package no.nav.tiltakspenger.datadeling.vedtak.infra.db
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.datadeling.vedtak.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakRepoTest {

    @Test
    fun `gammelt vedtak uten innvilgelsesperiode leses med virkningsperioden som innvilgelsesperiode`() {
        // Rammevedtak lagret før innvilgelsesperiode-kolonnen ble tatt i bruk har den som null.
        // For innvilgede vedtak var virkningsperioden den gang også innvilgelsesperioden.
        withMigratedDb { testDataHelper ->
            val sak = SakMother.sak(fnr = Fnr.random())
            testDataHelper.sakRepo.lagre(sak)
            val fraOgMed = LocalDate.of(2024, 1, 1)
            val tilOgMed = LocalDate.of(2024, 1, 31)

            testDataHelper.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        """
                        insert into rammevedtak (
                          vedtak_id, sak_id, fra_og_med, til_og_med, rettighet,
                          opprettet_tidspunkt, mottatt_tidspunkt, innvilgelsesperiode
                        ) values (
                          :vedtak_id, :sak_id, :fra_og_med, :til_og_med, 'TILTAKSPENGER',
                          :tidspunkt, :tidspunkt, null
                        )
                        """.trimIndent(),
                        mapOf(
                            "vedtak_id" to "gammelt-vedtak",
                            "sak_id" to sak.id.toString(),
                            "fra_og_med" to fraOgMed,
                            "til_og_med" to tilOgMed,
                            "tidspunkt" to LocalDateTime.parse("2024-01-01T00:00:00"),
                        ),
                    ).asUpdate,
                )
            }

            val vedtak = testDataHelper.vedtakRepo.hentForFnr(sak.fnr).single().vedtak
            vedtak.virkningsperiode shouldBe Periode(fraOgMed, tilOgMed)
            vedtak.innvilgelsesperiode shouldBe Periode(fraOgMed, tilOgMed)
        }
    }

    @Test
    fun `hentSakMedVedtakForFnr gir saken med alle vedtakene, og null nar personen ikke har vedtak`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)

            vedtakRepo.hentSakMedVedtakForFnr(fnr) shouldBe null

            sakRepo.lagre(sak)
            val førsteVedtak = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                virkningsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)),
            )
            val andreVedtak = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                virkningsperiode = Periode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)),
            )
            vedtakRepo.lagre(førsteVedtak)
            vedtakRepo.lagre(andreVedtak)

            val sakMedVedtak = vedtakRepo.hentSakMedVedtakForFnr(fnr)!!
            sakMedVedtak.sak shouldBe sak
            sakMedVedtak.vedtak shouldContainExactlyInAnyOrder listOf(førsteVedtak, andreVedtak)
        }
    }

    @Test
    fun `markerSendtTilObo tar vedtaket ut av koen for deling`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val vedtak = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
            )
            vedtakRepo.lagre(vedtak)

            vedtakRepo.hentRammevedtakSomSkalDelesMedObo().map { it.vedtak.vedtakId } shouldBe listOf(vedtak.vedtakId)

            vedtakRepo.markerSendtTilObo(vedtak.vedtakId, LocalDateTime.parse("2024-03-01T10:00:00"))

            vedtakRepo.hentRammevedtakSomSkalDelesMedObo().map { it.vedtak.vedtakId } shouldBe emptyList()
        }
    }

    @Test
    fun `kan lagre og hente vedtak`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val vedtak = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
            )
            vedtakRepo.lagre(vedtak)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakId(vedtak.vedtakId, session)?.vedtak shouldBe vedtak
            }
            vedtakRepo.hentForFnr(fnr).map { it.vedtak } shouldBe listOf(vedtak)
            vedtakRepo.hentRammevedtakSomSkalDelesMedObo(limit = 1).map { it.vedtak } shouldBe listOf(vedtak)

            val enDagFørFraOgMed = vedtak.virkningsperiode.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = vedtak.virkningsperiode.tilOgMed.plusDays(1)

            // periode før vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(enDagFørFraOgMed, enDagFørFraOgMed),
            ) shouldBe emptyList()
            // periode første dag i vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(vedtak.virkningsperiode.fraOgMed, vedtak.virkningsperiode.fraOgMed),
            ).map { it.vedtak } shouldBe listOf(vedtak)
            // periode siste dag i vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(vedtak.virkningsperiode.tilOgMed, vedtak.virkningsperiode.tilOgMed),
            ).map { it.vedtak } shouldBe listOf(vedtak)
            // periode etter vedtak
            vedtakRepo.hentForFnrOgPeriode(
                fnr,
                Periode(enDagEtterTilOgMed, enDagEtterTilOgMed),
            ) shouldBe emptyList()
        }
    }

    @Test
    fun `kan lagre og hente vedtak med barnetillegg`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val virkningsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
            val vedtakMedBarnetillegg = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                virkningsperiode = virkningsperiode,
                barnetillegg = Barnetillegg(
                    perioder = listOf(
                        BarnetilleggPeriode(
                            antallBarn = 1,
                            periode = Periode(virkningsperiode.fraOgMed, virkningsperiode.tilOgMed),
                        ),
                    ),
                ),
                rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            )
            vedtakRepo.lagre(vedtakMedBarnetillegg)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakId(
                    vedtakMedBarnetillegg.vedtakId,
                    session,
                )?.vedtak shouldBe vedtakMedBarnetillegg
            }
        }
    }

    @Test
    fun `kan lagre og hente avslagsvedtak`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val vedtakRepo = testDataHelper.vedtakRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val vedtak = VedtakMother.tiltakspengerVedtak(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET),
            )
            vedtakRepo.lagre(vedtak)

            testDataHelper.sessionFactory.withSession { session ->
                vedtakRepo.hentForVedtakId(vedtak.vedtakId, session)?.vedtak shouldBe vedtak
            }
        }
    }
}
