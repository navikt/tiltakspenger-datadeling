package no.nav.tiltakspenger.datadeling.motta.infra.db

import kotliquery.Session
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyttVedtakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

internal class MottaNyttVedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MottaNyttVedtakRepo {
    val log = KotlinLogging.logger { }

    override fun lagre(vedtak: TiltakspengerVedtak) {
        sessionFactory.withTransaction { session ->
            log.info { "Sletter eksisterende vedtak med id ${vedtak.vedtakId} hvis den finnes" }
            slettEksisterende(vedtak.vedtakId, session).also {
                log.info { "Slettet $it rader. Lagrer vedtak med id ${vedtak.vedtakId}" }
            }
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
    }

    private fun slettEksisterende(
        vedtakId: String,
        session: Session,
    ): Int {
        return session.run(
            queryOf(
                "delete from rammevedtak where vedtak_id = :vedtak_id",
                mapOf(
                    "vedtak_id" to vedtakId,
                ),
            ).asUpdate,
        )
    }

    fun hentForVedtakIdOgKilde(
        vedtakId: String,
        kilde: String,
        session: Session,
    ): TiltakspengerVedtak? {
        return session.run(
            queryOf(
                "select * from rammevedtak where vedtak_id = :vedtak_id and kilde = :kilde",
                mapOf(
                    "vedtak_id" to vedtakId,
                    "kilde" to kilde,
                ),
            ).map {
                val kildeIDB = it.string("kilde")
                require(kildeIDB == kilde) { "Forventet kilde $kilde, men var $kildeIDB" }
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
                    mottattTidspunkt = it.localDateTime("mottatt_tidspunkt"),
                    opprettetTidspunkt = it.localDateTime("opprettet_tidspunkt"),
                )
            }.asSingle,
        )
    }
}
