package no.nav.tiltakspenger.datadeling.motta.infra.db

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.app.KunneIkkeLagreVedtak
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

internal class MottaNyttVedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MottaNyttVedtakRepo {
    val log = KotlinLogging.logger { }

    override fun lagre(vedtak: TiltakspengerVedtak): Either<KunneIkkeLagreVedtak, Unit> {
        log.info { "Lagrer vedtak med kilde ${vedtak.kilde} og id ${vedtak.vedtakId}" }
        if (eksisterer(vedtak.vedtakId, vedtak.kilde)) {
            log.info { "Dedup: Vedtak med kilde ${vedtak.kilde} og id ${vedtak.vedtakId} finnes allerede." }
            return KunneIkkeLagreVedtak.AlleredeLagret.left()
        }
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    insert into rammevedtak (
                      vedtak_id,
                      sak_id,
                      saksnummer,
                      fnr,
                      fra_og_med,
                      til_og_med,
                      antall_dager_per_meldeperiode,
                      meldeperiodens_lengde,
                      dagsats_tiltakspenger,
                      dagsats_barnetillegg,
                      antall_barn,
                      tiltaksgjennomføring_id,
                      rettighet,
                      kilde,
                      opprettet_tidspunkt,
                      mottatt_tidspunkt
                    ) values (
                      :vedtak_id,
                      :sak_id,
                      :saksnummer,
                      :fnr,
                      :fra_og_med,
                      :til_og_med,
                      :antall_dager_per_meldeperiode,
                      :meldeperiodens_lengde,
                      :dagsats_tiltakspenger,
                      :dagsats_barnetillegg,
                      :antall_barn,
                      :tiltaksgjennomforing_id,
                      :rettighet,
                      :kilde,
                      :opprettet_tidspunkt,
                      :mottatt_tidspunkt
                    )
                    """.trimIndent(),
                    mapOf(
                        "vedtak_id" to vedtak.vedtakId,
                        "sak_id" to vedtak.sakId,
                        "saksnummer" to vedtak.saksnummer,
                        "fnr" to vedtak.fnr.verdi,
                        "fra_og_med" to vedtak.periode.fraOgMed,
                        "til_og_med" to vedtak.periode.tilOgMed,
                        "antall_dager_per_meldeperiode" to vedtak.antallDagerPerMeldeperiode,
                        "meldeperiodens_lengde" to vedtak.meldeperiodensLengde,
                        "dagsats_tiltakspenger" to vedtak.dagsatsTiltakspenger,
                        "dagsats_barnetillegg" to vedtak.dagsatsBarnetillegg,
                        "antall_barn" to vedtak.antallBarn,
                        "tiltaksgjennomforing_id" to vedtak.tiltaksgjennomføringId,
                        "rettighet" to vedtak.rettighet.name,
                        "kilde" to vedtak.kilde,
                        "opprettet_tidspunkt" to vedtak.opprettetTidspunkt,
                        "mottatt_tidspunkt" to vedtak.mottattTidspunkt,
                    ),
                ).asUpdate,
            )
        }
        log.info { "Vedtak med kilde ${vedtak.kilde} og id ${vedtak.vedtakId} lagret." }
        return Unit.right()
    }

    private fun eksisterer(vedtakId: String, kilde: String): Boolean {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    "select count(*) from rammevedtak where vedtak_id = :vedtak_id and kilde = :kilde",
                    mapOf(
                        "vedtak_id" to vedtakId,
                        "kilde" to kilde,
                    ),
                ).map {
                    it.int(1)
                }.asSingle,
            ) == 1
        }
    }

    fun hentForVedtakIdOgKilde(
        vedtakId: String,
        kilde: String,
    ): TiltakspengerVedtak? {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    "select * from rammevedtak where vedtak_id = :vedtak_id and kilde = :kilde",
                    mapOf(
                        "vedtak_id" to vedtakId,
                        "kilde" to kilde,
                    ),
                ).map {
                    TiltakspengerVedtak(
                        vedtakId = it.string("vedtak_id"),
                        sakId = it.string("sak_id"),
                        saksnummer = it.string("saksnummer"),
                        fnr = Fnr.fromString(it.string("fnr")),
                        periode = Periode(
                            it.localDate("fra_og_med"),
                            it.localDate("til_og_med"),
                        ),
                        antallDagerPerMeldeperiode = it.int("antall_dager_per_meldeperiode"),
                        meldeperiodensLengde = it.int("meldeperiodens_lengde"),
                        dagsatsTiltakspenger = it.int("dagsats_tiltakspenger"),
                        dagsatsBarnetillegg = it.int("dagsats_barnetillegg"),
                        antallBarn = it.int("antall_barn"),
                        tiltaksgjennomføringId = it.string("tiltaksgjennomføring_id"),
                        // TODO post-mvp jah: Lag egen db-mapping her.
                        rettighet = TiltakspengerVedtak.Rettighet.valueOf(it.string("rettighet")),
                        kilde = it.string("kilde"),
                        mottattTidspunkt = it.localDateTime("mottatt_tidspunkt"),
                        opprettetTidspunkt = it.localDateTime("opprettet_tidspunkt"),
                    )
                }.asSingle,
            )
        }
    }
}
