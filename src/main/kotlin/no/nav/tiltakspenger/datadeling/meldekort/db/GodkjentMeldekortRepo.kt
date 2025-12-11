package no.nav.tiltakspenger.datadeling.meldekort.db

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.application.db.prefixColumn
import no.nav.tiltakspenger.datadeling.application.db.toPGObject
import no.nav.tiltakspenger.datadeling.meldekort.domene.GodkjentMeldekort
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery

class GodkjentMeldekortRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    val log = KotlinLogging.logger { }

    companion object {
        fun godkjentMeldekortFromRow(row: Row, alias: String? = null): GodkjentMeldekort {
            val col = prefixColumn(alias)
            return GodkjentMeldekort(
                meldekortbehandlingId = MeldekortId.fromString(row.string(col("meldekortbehandling_id"))),
                kjedeId = row.string(col("kjede_id")),
                sakId = SakId.fromString(row.string(col("sak_id"))),
                meldeperiodeId = MeldeperiodeId.fromString(row.string(col("meldeperiode_id"))),
                mottattTidspunkt = row.localDateTimeOrNull(col("mottatt_tidspunkt")),
                vedtattTidspunkt = row.localDateTime(col("vedtatt_tidspunkt")),
                behandletAutomatisk = row.boolean(col("behandlet_automatisk")),
                korrigert = row.boolean(col("korrigert")),
                fraOgMed = row.localDate(col("fra_og_med")),
                tilOgMed = row.localDate(col("til_og_med")),
                meldekortdager = objectMapper.readValue<List<GodkjentMeldekort.MeldekortDag>>(row.string(col("meldekortdager"))),
                journalpostId = row.string(col("journalpost_id")),
                totaltBelop = row.int(col("totalt_belop")),
                totalDifferanse = row.intOrNull(col("total_differanse")),
                barnetillegg = row.boolean(col("barnetillegg")),
                opprettet = row.localDateTime(col("opprettet")),
                sistEndret = row.localDateTime(col("sist_endret")),
            )
        }
    }

    fun lagre(meldekort: GodkjentMeldekort) {
        sessionFactory.withTransaction { session ->
            session.run(
                sqlQuery(
                    """
                    insert into godkjent_meldekort (
                        meldekortbehandling_id,
                        kjede_id,
                        sak_id,
                        meldeperiode_id,
                        mottatt_tidspunkt,
                        vedtatt_tidspunkt,
                        behandlet_automatisk,
                        korrigert,
                        fra_og_med,
                        til_og_med,
                        meldekortdager,
                        journalpost_id,
                        totalt_belop,
                        total_differanse,
                        barnetillegg,
                        opprettet,
                        sist_endret
                    ) values (
                        :meldekortbehandling_id,
                        :kjede_id,
                        :sak_id,
                        :meldeperiode_id,
                        :mottatt_tidspunkt,
                        :vedtatt_tidspunkt,
                        :behandlet_automatisk,
                        :korrigert,
                        :fra_og_med,
                        :til_og_med,
                        :meldekortdager,
                        :journalpost_id,
                        :totalt_belop,
                        :total_differanse,
                        :barnetillegg,
                        :opprettet,
                        :sist_endret
                    )
                    on conflict (meldekortbehandling_id) do update set
                        kjede_id = :kjede_id,
                        sak_id = :sak_id,
                        meldeperiode_id = :meldeperiode_id,
                        mottatt_tidspunkt = :mottatt_tidspunkt,
                        vedtatt_tidspunkt = :vedtatt_tidspunkt,
                        behandlet_automatisk = :behandlet_automatisk,
                        korrigert = :korrigert,
                        fra_og_med = :fra_og_med,
                        til_og_med = :til_og_med,
                        meldekortdager = :meldekortdager,
                        journalpost_id = :journalpost_id,
                        totalt_belop = :totalt_belop,
                        total_differanse = :total_differanse,
                        barnetillegg = :barnetillegg,
                        opprettet = :opprettet,
                        sist_endret = :sist_endret
                """,
                    "meldekortbehandling_id" to meldekort.meldekortbehandlingId.toString(),
                    "kjede_id" to meldekort.kjedeId,
                    "sak_id" to meldekort.sakId.toString(),
                    "meldeperiode_id" to meldekort.meldeperiodeId.toString(),
                    "mottatt_tidspunkt" to meldekort.mottattTidspunkt,
                    "vedtatt_tidspunkt" to meldekort.vedtattTidspunkt,
                    "behandlet_automatisk" to meldekort.behandletAutomatisk,
                    "korrigert" to meldekort.korrigert,
                    "fra_og_med" to meldekort.fraOgMed,
                    "til_og_med" to meldekort.tilOgMed,
                    "meldekortdager" to toPGObject(meldekort.meldekortdager),
                    "journalpost_id" to meldekort.journalpostId,
                    "totalt_belop" to meldekort.totaltBelop,
                    "total_differanse" to meldekort.totalDifferanse,
                    "barnetillegg" to meldekort.barnetillegg,
                    "opprettet" to meldekort.opprettet,
                    "sist_endret" to meldekort.sistEndret,
                ).asUpdate,
            )
        }
        log.info { "Lagret godkjent meldekort for meldekortbehandlingId ${meldekort.meldekortbehandlingId}, meldeperiodeId ${meldekort.meldeperiodeId} for kjedeId ${meldekort.kjedeId}, sakId ${meldekort.sakId}" }
    }

    fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
    ): List<GodkjentMeldekort> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select gm.*,
                      s.fnr as sak_fnr
                    from godkjent_meldekort gm join sak s on s.id = gm.sak_id
                    where s.fnr = :fnr
                      and fra_og_med <= :til_og_med
                      and til_og_med >= :fra_og_med
                    """.trimIndent(),
                    mapOf(
                        "fra_og_med" to periode.fraOgMed,
                        "til_og_med" to periode.tilOgMed,
                        "fnr" to fnr.verdi,
                    ),
                ).map {
                    godkjentMeldekortFromRow(it)
                }.asList,
            )
        }
    }
}
