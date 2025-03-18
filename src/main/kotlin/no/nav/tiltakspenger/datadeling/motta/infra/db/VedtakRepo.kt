package no.nav.tiltakspenger.datadeling.motta.infra.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

class VedtakRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    val log = KotlinLogging.logger { }

    fun lagre(vedtak: TiltakspengerVedtak) {
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
                      rettighet,
                      kilde,
                      opprettet_tidspunkt,
                      mottatt_tidspunkt,
                      barnetillegg
                    ) values (
                      :vedtak_id,
                      :sak_id,
                      :saksnummer,
                      :fnr,
                      :fra_og_med,
                      :til_og_med,
                      :antall_dager_per_meldeperiode,
                      :rettighet,
                      :kilde,
                      :opprettet_tidspunkt,
                      :mottatt_tidspunkt,
                      :barnetillegg
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
                        "rettighet" to vedtak.rettighet.name,
                        "kilde" to vedtak.kilde.navn,
                        "opprettet_tidspunkt" to vedtak.opprettet,
                        "mottatt_tidspunkt" to vedtak.mottattTidspunkt,
                        "barnetillegg" to toPGObject(vedtak.barnetillegg),
                    ),
                ).asUpdate,
            )
        }
        log.info { "Vedtak med kilde ${vedtak.kilde.navn} og id ${vedtak.vedtakId} lagret." }
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

    fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
        kilde: Kilde,
    ): List<TiltakspengerVedtak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select * from rammevedtak 
                      where fnr = :fnr 
                      and kilde = :kilde
                      and fra_og_med <= :tilOgMed 
                      and til_og_med >= :fraOgMed
                    """.trimIndent(),
                    mapOf(
                        "fraOgMed" to periode.fraOgMed,
                        "tilOgMed" to periode.tilOgMed,
                        "fnr" to fnr.verdi,
                        // TODO post-mvp jah: Mangler kilde i databaseindeksen
                        "kilde" to kilde.navn,
                    ),
                ).map {
                    val kildeFraDatabase = it.string("kilde")
                    require(kildeFraDatabase == kilde.navn) { "Forventet kilde ${kilde.navn}, men var $kildeFraDatabase" }
                    fromRow(it)
                }.asList,
            )
        }
    }

    internal fun hentForVedtakIdOgKilde(
        vedtakId: String,
        kilde: Kilde,
        session: Session,
    ): TiltakspengerVedtak? {
        return session.run(
            queryOf(
                "select * from rammevedtak where vedtak_id = :vedtak_id and kilde = :kilde",
                mapOf(
                    "vedtak_id" to vedtakId,
                    "kilde" to kilde.navn,
                ),
            ).map {
                val kildeFraDatabase = it.string("kilde")
                require(kildeFraDatabase == kilde.navn) { "Forventet kilde ${kilde.navn}, men var $kildeFraDatabase" }
                fromRow(it)
            }.asSingle,
        )
    }

    private fun fromRow(row: Row): TiltakspengerVedtak = TiltakspengerVedtak(
        vedtakId = row.string("vedtak_id"),
        sakId = row.string("sak_id"),
        saksnummer = row.string("saksnummer"),
        fnr = Fnr.fromString(row.string("fnr")),
        periode = Periode(
            row.localDate("fra_og_med"),
            row.localDate("til_og_med"),
        ),
        antallDagerPerMeldeperiode = row.int("antall_dager_per_meldeperiode"),
        // TODO post-mvp jah: Lag egen db-mapping her.
        rettighet = TiltakspengerVedtak.Rettighet.valueOf(row.string("rettighet")),
        mottattTidspunkt = row.localDateTime("mottatt_tidspunkt"),
        opprettet = row.localDateTime("opprettet_tidspunkt"),
        barnetillegg = row.stringOrNull("barnetillegg")?.let { objectMapper.readValue<Barnetillegg>(it) },
    )
}
