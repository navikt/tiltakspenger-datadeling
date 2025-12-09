package no.nav.tiltakspenger.datadeling.meldekort.db

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.application.db.prefixColumn
import no.nav.tiltakspenger.datadeling.application.db.toPGObject
import no.nav.tiltakspenger.datadeling.meldekort.domene.GodkjentMeldekort
import no.nav.tiltakspenger.libs.common.Fnr
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
                        opprettet,
                        sist_endret
                    ) values (
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
                        :opprettet,
                        :sist_endret
                    )
                    on conflict (kjede_id, sak_id) do update set
                        meldeperiode_id = :meldeperiode_id,
                        mottatt_tidspunkt = :mottatt_tidspunkt,
                        vedtatt_tidspunkt = :vedtatt_tidspunkt,
                        behandlet_automatisk = :behandlet_automatisk,
                        korrigert = :korrigert,
                        fra_og_med = :fra_og_med,
                        til_og_med = :til_og_med,
                        meldekortdager = :meldekortdager,
                        opprettet = :opprettet,
                        sist_endret = :sist_endret
                """,
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
                    "opprettet" to meldekort.opprettet,
                    "sist_endret" to meldekort.sistEndret,
                ).asUpdate,
            )
        }
        log.info { "Lagret godkjent meldekort for meldeperiodeId ${meldekort.meldeperiodeId} for kjedeId ${meldekort.kjedeId}, sakId ${meldekort.sakId}" }
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
